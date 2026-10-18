package com.tikahome.integration;

import com.anthropic.models.beta.messages.BetaMessageParam;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.tikahome.TestcontainersConfiguration;
import com.tikahome.entity.AssistantMessage;
import com.tikahome.entity.AssistantRole;
import com.tikahome.exception.AssistantUnavailableException;
import com.tikahome.model.ClaudeResponse;
import com.tikahome.model.TokenUsage;
import com.tikahome.repository.AssistantConversationRepository;
import com.tikahome.repository.AssistantMessageRepository;
import com.tikahome.service.ClaudeGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The chat endpoint over real HTTP (Server-Sent Events) and a real Postgres. Claude itself is mocked:
 * these tests cost nothing and don't need an API key. Small limits make them quick to reach.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "ANTHROPIC_API_KEY=test-fixture-not-a-real-key",
        "tikahome.assistant.max-messages-per-conversation=2",
        "tikahome.assistant.max-messages-per-hour-per-visitor=3",
        "tikahome.assistant.daily-budget-usd=1.00"})
@Import(TestcontainersConfiguration.class)
class AssistantChatIT {

    static final String PROPERTY = "6f1c2b8e-4d3a-4f5e-9a7b-1c2d3e4f5a6b"; // seed data (V3)
    static final Pattern CONVERSATION_ID = Pattern.compile("\"conversationId\":\"([0-9a-f-]{36})\"");
    static final TokenUsage SMALL = new TokenUsage(1000, 100, 0, 0); // 0.004 + 0.002 = 0.006 USD

    @LocalServerPort
    int port;

    @MockitoBean
    ClaudeGateway claude;

    @Autowired
    AssistantConversationRepository conversations;

    @Autowired
    AssistantMessageRepository messages;

    final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void clean() {
        messages.deleteAllInBatch();
        conversations.deleteAllInBatch();
    }

    @Test
    void streamsTheAnswerAndLogsTheConversation() throws Exception {
        claudeAnswers(SMALL, "Yes, there is ", "a pool.");

        var first = chat(null, "Is there a pool?");

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.headers().firstValue("Content-Type").orElseThrow()).startsWith("text/event-stream");
        assertThat(first.body())
                .contains("event:start", "event:delta", "\"text\":\"Yes, there is \"", "\"text\":\"a pool.\"",
                        "event:done", "\"refused\":false")
                .doesNotContain("event:error");
        String conversationId = conversationId(first.body());

        var conversation = conversations.findAll().getFirst();
        assertThat(conversation.getId().toString()).isEqualTo(conversationId);
        assertThat(conversation.getGuestMessages()).isEqualTo(1);
        assertThat(conversation.getCostUsd()).isEqualByComparingTo("0.006");
        assertThat(conversation.getVisitorHash()).hasSize(64).doesNotContain("127.0.0.1");
        List<AssistantMessage> logged = messages.findAll().stream()
                .sorted(Comparator.comparing(AssistantMessage::getCreatedAt)).toList();
        assertThat(logged).extracting(AssistantMessage::getRole).containsExactly(AssistantRole.GUEST, AssistantRole.ASSISTANT);
        assertThat(logged.get(1).getText()).isEqualTo("Yes, there is a pool.");
        assertThat(logged.get(1).getInputTokens()).isEqualTo(1000);

        // Second message: the server replays the first exchange from its own log.
        var second = chat(conversationId, "Is it heated?");

        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(conversationId(second.body())).isEqualTo(conversationId);
        ArgumentCaptor<MessageCreateParams> params = ArgumentCaptor.forClass(MessageCreateParams.class);
        verify(claude, times(2)).stream(params.capture(), any());
        List<BetaMessageParam> sent = params.getAllValues().get(1).messages();
        assertThat(sent).hasSize(3);
        assertThat(sent.get(0).content().string()).hasValue("Is there a pool?");
        assertThat(sent.get(1).content().string()).hasValue("Yes, there is a pool.");
    }

    @Test
    void limitsMessagesPerConversationThenPerHour() throws Exception {
        claudeAnswers(SMALL, "OK");
        String conversationId = conversationId(chat(null, "one").body());
        assertThat(chat(conversationId, "two").statusCode()).isEqualTo(200);

        var tooLong = chat(conversationId, "three");
        assertThat(tooLong.statusCode()).isEqualTo(429);
        assertThat(tooLong.body()).contains("\"limit\":\"CONVERSATION\"");

        assertThat(chat(null, "three, new conversation").statusCode()).isEqualTo(200);

        var tooMany = chat(null, "four");
        assertThat(tooMany.statusCode()).isEqualTo(429);
        assertThat(tooMany.body()).contains("\"limit\":\"HOURLY\"");
        assertThat(tooMany.headers().firstValue("Retry-After")).isPresent();
        verify(claude, times(3)).stream(any(), any());
    }

    @Test
    void stopsForTheDayWhenTheBudgetIsSpent() throws Exception {
        claudeAnswers(new TokenUsage(1000, 60_000, 0, 0), "A long answer"); // ~1.20 USD > 1.00
        assertThat(status()).contains("\"available\":true");

        assertThat(chat(null, "first").statusCode()).isEqualTo(200);
        var refused = chat(null, "second");

        assertThat(refused.statusCode()).isEqualTo(503);
        assertThat(refused.body()).contains("\"reason\":\"DAILY_BUDGET\"");
        assertThat(status()).contains("\"available\":false");
    }

    @Test
    void claudeFailureDuringStreamingIsAnErrorEventAndIsLogged() throws Exception {
        when(claude.stream(any(), any())).thenAnswer(invocation -> {
            Consumer<String> onText = invocation.getArgument(1);
            onText.accept("Partial ");
            throw new AssistantUnavailableException(AssistantUnavailableException.Reason.CLAUDE_ERROR, "boom");
        });

        var response = chat(null, "Hello");

        assertThat(response.statusCode()).isEqualTo(200); // streaming had already started
        assertThat(response.body()).contains("event:error", "\"reason\":\"CLAUDE_ERROR\"").doesNotContain("event:done");
        assertThat(messages.findAll()).filteredOn(AssistantMessage::isFailed)
                .singleElement().extracting(AssistantMessage::getText).isEqualTo("Partial ");
    }

    @Test
    void unknownConversationIs404() throws Exception {
        var response = chat("00000000-0000-0000-0000-000000000000", "Hello");

        assertThat(response.statusCode()).isEqualTo(404);
    }

    private void claudeAnswers(TokenUsage usage, String... chunks) {
        when(claude.stream(any(), any())).thenAnswer(invocation -> {
            Consumer<String> onText = invocation.getArgument(1);
            for (String chunk : chunks) {
                onText.accept(chunk);
            }
            String text = String.join("", chunks);
            return new ClaudeResponse(text, List.of(), ClaudeResponse.Stop.END_TURN, usage,
                    BetaMessageParam.builder().role(BetaMessageParam.Role.ASSISTANT).content(text).build());
        });
    }

    private HttpResponse<String> chat(String conversationId, String message) throws Exception {
        String body = """
                {"conversationId":%s,"message":"%s","language":"en"}"""
                .formatted(conversationId == null ? "null" : "\"" + conversationId + "\"", message);
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/properties/" + PROPERTY + "/assistant/chat"))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private String status() throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/properties/" + PROPERTY + "/assistant"))
                .build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    private static String conversationId(String sse) {
        Matcher matcher = CONVERSATION_ID.matcher(sse);
        assertThat(matcher.find()).as("start event with conversationId in: %s", sse).isTrue();
        return matcher.group(1);
    }
}
