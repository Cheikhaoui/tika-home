package com.tikahome.service;

import com.anthropic.errors.InternalServerException;
import com.anthropic.models.beta.messages.BetaMessageParam;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.tikahome.config.AssistantProperties;
import com.tikahome.exception.AssistantUnavailableException;
import com.tikahome.model.AssistantResult;
import com.tikahome.model.ChatTurn;
import com.tikahome.model.ClaudeResponse;
import com.tikahome.model.DateRange;
import com.tikahome.model.KnowledgeChunk;
import com.tikahome.model.NightPrice;
import com.tikahome.model.Pricing;
import com.tikahome.model.Quote;
import com.tikahome.model.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssistantServiceTest {

    final UUID propertyId = UUID.randomUUID();
    final KnowledgeRetriever retriever = mock(KnowledgeRetriever.class);
    final QuoteService quoteService = mock(QuoteService.class);
    final AssistantTools tools = new AssistantTools(quoteService, mock(AvailabilityService.class));
    final FakeClaude claude = new FakeClaude();
    final AssistantProperties properties = new AssistantProperties("m", "t", "/tmp", 4, 0.35,
            "claude-opus-5-5", "low", 2048, 20, 30, new BigDecimal("5.00"), "test-fixture-visitor-key");
    final Clock clock = Clock.fixed(Instant.parse("2027-03-01T10:00:00Z"), ZoneOffset.UTC);

    AssistantService service(String apiKey) {
        return new AssistantService(retriever, tools, claude, properties, JsonMapper.builder().build(), clock, apiKey);
    }

    @BeforeEach
    void knowledge() {
        when(retriever.retrieve(anyString())).thenReturn(List.of(
                new KnowledgeChunk("house.md", "Pool", "The plunge pool is not heated.")));
    }

    @Test
    void withoutApiKeyTheAssistantIsUnavailable() {
        assertThat(service("").isConfigured()).isFalse();
        assertThatThrownBy(() -> service("").reply(propertyId, List.of(), "Hi", "en", t -> {}))
                .isInstanceOf(AssistantUnavailableException.class);
    }

    @Test
    void answersWithKnowledgeTodayAndLanguageInTheUserTurn() {
        claude.answers.add(response("Non, la piscine n’est pas chauffée.", List.of(), ClaudeResponse.Stop.END_TURN));
        List<String> streamed = new ArrayList<>();

        AssistantResult result = service("key").reply(propertyId, List.of(), "La piscine est chauffée ?", "fr", streamed::add);

        assertThat(result.text()).isEqualTo("Non, la piscine n’est pas chauffée.");
        assertThat(streamed).containsExactly("Non, la piscine n’est pas chauffée.");
        assertThat(result.refused()).isFalse();
        MessageCreateParams params = claude.requests.getFirst();
        assertThat(params.model().toString()).isEqualTo("claude-opus-5-5");
        assertThat(params.tools().orElseThrow()).hasSize(2);
        assertThat(params.betas().orElseThrow().toString()).contains(AssistantService.FALLBACK_BETA);
        String userTurn = text(params.messages().getLast());
        assertThat(userTurn).contains("Today is 2027-03-01", "Answer in French",
                "<section title=\"Pool\">", "The plunge pool is not heated.", "La piscine est chauffée ?");
        assertThat(params.toString()).contains("You are the online assistant of Tika Home", "ephemeral");
    }

    @Test
    void runsToolsAndSendsResultsBackWithTheUnchangedAssistantTurn() {
        when(quoteService.quote(eq(propertyId), any(DateRange.class), anyInt())).thenReturn(new Quote(
                new DateRange(LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12")), 2, "EUR",
                new Pricing(List.of(new NightPrice(LocalDate.parse("2027-03-10"), new BigDecimal("120"), null),
                        new NightPrice(LocalDate.parse("2027-03-11"), new BigDecimal("120"), null)),
                        new BigDecimal("240"), new BigDecimal("40"), new BigDecimal("280"), 2), Set.of()));
        var toolTurn = response("Je vérifie… ", List.of(new ClaudeResponse.ToolCall("tu_1", AssistantTools.CHECK_STAY,
                Map.of("check_in", "2027-03-10", "check_out", "2027-03-12", "guests", 2))), ClaudeResponse.Stop.TOOL_USE);
        claude.answers.add(toolTurn);
        claude.answers.add(response("C’est libre : 280 € au total.", List.of(), ClaudeResponse.Stop.END_TURN));

        AssistantResult result = service("key").reply(propertyId, List.of(), "Libre du 10 au 12 mars ?", "fr", t -> {});

        assertThat(result.text()).isEqualTo("Je vérifie… C’est libre : 280 € au total.");
        assertThat(result.toolsUsed()).containsExactly(AssistantTools.CHECK_STAY);
        assertThat(result.usage()).isEqualTo(new TokenUsage(200, 20, 0, 0)); // two calls added up
        List<BetaMessageParam> second = claude.requests.get(1).messages();
        assertThat(second).hasSize(3);
        assertThat(second.get(1)).isSameAs(toolTurn.assistantTurn()); // replayed unchanged (append-only)
        assertThat(second.get(2).toString()).contains("tu_1", "\"total\":280", "\"bookable\":true");
    }

    @Test
    void refusalEndsWithAPoliteMessageInTheGuestLanguage() {
        claude.answers.add(response("", List.of(), ClaudeResponse.Stop.REFUSAL));

        AssistantResult result = service("key").reply(propertyId, List.of(), "…", "ar", t -> {});

        assertThat(result.refused()).isTrue();
        assertThat(result.text()).isEqualTo(AssistantService.refusalText("ar"));
    }

    @Test
    void historyIsReplayedAsAlternatingPlainText() {
        claude.answers.add(response("OK", List.of(), ClaudeResponse.Stop.END_TURN));
        var history = List.of(new ChatTurn("assistant", "orphan"), new ChatTurn("user", "Hi"),
                new ChatTurn("assistant", "Hello!"), new ChatTurn("user", "unanswered"));

        service("key").reply(propertyId, history, "Pool?", "en", t -> {});

        List<BetaMessageParam> messages = claude.requests.getFirst().messages();
        assertThat(messages).extracting(m -> m.role().toString()).containsExactly("user", "assistant", "user");
        assertThat(text(messages.get(0))).isEqualTo("Hi");
        assertThat(text(messages.get(1))).isEqualTo("Hello!");
    }

    @Test
    void claudeOutageBecomesAssistantUnavailable() {
        claude.failure = InternalServerException.builder()
                .statusCode(500)
                .headers(com.anthropic.core.http.Headers.builder().build())
                .body(com.anthropic.core.JsonValue.from(Map.of()))
                .build();

        assertThatThrownBy(() -> service("key").reply(propertyId, List.of(), "Hi", "en", t -> {}))
                .isInstanceOf(AssistantUnavailableException.class);
    }

    private static String text(BetaMessageParam message) {
        return message.content().string().orElseThrow();
    }

    private static ClaudeResponse response(String text, List<ClaudeResponse.ToolCall> calls, ClaudeResponse.Stop stop) {
        return new ClaudeResponse(text, calls, stop, new TokenUsage(100, 10, 0, 0),
                BetaMessageParam.builder().role(BetaMessageParam.Role.ASSISTANT).content(text.isEmpty() ? "…" : text).build());
    }

    /** Scripted Claude: returns queued answers and records every request. */
    static class FakeClaude implements ClaudeGateway {
        final Deque<ClaudeResponse> answers = new ArrayDeque<>();
        final List<MessageCreateParams> requests = new ArrayList<>();
        RuntimeException failure;

        @Override
        public ClaudeResponse stream(MessageCreateParams params, Consumer<String> onText) {
            requests.add(params);
            if (failure != null) throw failure;
            ClaudeResponse next = answers.removeFirst();
            if (!next.text().isEmpty()) onText.accept(next.text());
            return next;
        }
    }
}
