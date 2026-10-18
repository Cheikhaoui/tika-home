package com.tikahome.service;

import com.tikahome.config.AssistantProperties;
import com.tikahome.entity.AssistantConversation;
import com.tikahome.entity.AssistantMessage;
import com.tikahome.entity.AssistantRole;
import com.tikahome.exception.AssistantLimitException;
import com.tikahome.exception.AssistantUnavailableException;
import com.tikahome.exception.NotFoundException;
import com.tikahome.model.AssistantResult;
import com.tikahome.model.ChatContext;
import com.tikahome.model.ChatTurn;
import com.tikahome.repository.AssistantConversationRepository;
import com.tikahome.repository.AssistantMessageRepository;
import com.tikahome.repository.PropertyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A guest chat message from start to finish, in two steps so no database transaction stays open while
 * Claude is writing (that can take many seconds):
 * 1. {@link #accept}: one short transaction - checks (configured, daily budget, rate limits), stores the
 *    guest message, loads the earlier turns.
 * 2. {@link #answer}: calls Claude outside any transaction, then stores the answer with its tokens and cost.
 * The limits are checked before each message, so concurrent messages can overshoot them slightly: fine for
 * cost protection, and much simpler than locking.
 */
@Service
public class AssistantChatService {

    private static final Logger log = LoggerFactory.getLogger(AssistantChatService.class);
    private static final ZoneId MARRAKECH = ZoneId.of("Africa/Casablanca");
    private static final Duration HOUR = Duration.ofHours(1);

    private final AssistantService assistant;
    private final AssistantConversationRepository conversations;
    private final AssistantMessageRepository messages;
    private final PropertyRepository properties;
    private final VisitorHasher visitorHasher;
    private final AssistantProperties settings;
    private final TransactionTemplate tx;
    private final Clock clock;

    public AssistantChatService(AssistantService assistant, AssistantConversationRepository conversations,
                                AssistantMessageRepository messages, PropertyRepository properties,
                                VisitorHasher visitorHasher, AssistantProperties settings, TransactionTemplate tx,
                                Clock clock) {
        this.assistant = assistant;
        this.conversations = conversations;
        this.messages = messages;
        this.properties = properties;
        this.visitorHasher = visitorHasher;
        this.settings = settings;
        this.tx = tx;
        this.clock = clock;
    }

    public boolean isAvailable() {
        return assistant.isConfigured() && !budgetReached();
    }

    /** Step 1. {@code conversationId} null = start a new conversation. */
    @Transactional
    public ChatContext accept(UUID propertyId, UUID conversationId, String ipAddress, String question, String language) {
        if (!assistant.isConfigured()) {
            throw new AssistantUnavailableException(AssistantUnavailableException.Reason.NOT_CONFIGURED,
                    "The assistant is not configured");
        }
        if (!properties.existsById(propertyId)) {
            throw new NotFoundException("Property not found: " + propertyId);
        }
        if (budgetReached()) {
            log.warn("Assistant daily budget of {} USD reached", settings.dailyBudgetUsd());
            throw new AssistantUnavailableException(AssistantUnavailableException.Reason.DAILY_BUDGET,
                    "The assistant is resting until tomorrow");
        }
        Instant now = clock.instant();
        String visitorHash = visitorHasher.hash(ipAddress);
        List<Instant> lastHour = messages.guestMessageTimesSince(visitorHash, now.minus(HOUR));
        if (lastHour.size() >= settings.maxMessagesPerHourPerVisitor()) {
            throw new AssistantLimitException(AssistantLimitException.Limit.HOURLY, lastHour.getFirst().plus(HOUR));
        }

        AssistantConversation conversation = conversationId == null
                ? conversations.save(new AssistantConversation(propertyId, visitorHash, language, now))
                : conversations.findById(conversationId)
                        .filter(c -> c.getPropertyId().equals(propertyId))
                        .orElseThrow(() -> new NotFoundException("Conversation not found"));
        if (conversation.getGuestMessages() >= settings.maxMessagesPerConversation()) {
            throw new AssistantLimitException(AssistantLimitException.Limit.CONVERSATION, null);
        }
        List<ChatTurn> history = history(conversation.getId());
        conversation.guestMessage(language, now);
        messages.save(AssistantMessage.guest(conversation.getId(), question, now));
        return new ChatContext(propertyId, conversation.getId(), history, question, language);
    }

    /** Step 2. Streams the answer to {@code onText}; the answer (or the failure) is always logged. */
    public AssistantResult answer(ChatContext chat, Consumer<String> onText) {
        StringBuilder streamed = new StringBuilder();
        try {
            AssistantResult result = assistant.reply(chat.propertyId(), chat.history(), chat.question(),
                    chat.language(), text -> {
                        streamed.append(text);
                        onText.accept(text);
                    });
            AssistantMessage answer = AssistantMessage.answer(chat.conversationId(), result, clock.instant());
            tx.executeWithoutResult(status -> {
                messages.save(answer);
                conversations.findById(chat.conversationId()).ifPresent(c -> c.addCost(answer.getCostUsd()));
            });
            return result;
        } catch (RuntimeException e) {
            // Claude failed, or the guest closed the page (sending the next piece of text failed).
            tx.executeWithoutResult(status ->
                    messages.save(AssistantMessage.failure(chat.conversationId(), streamed.toString(), clock.instant())));
            throw e;
        }
    }

    private boolean budgetReached() {
        Instant startOfDay = LocalDate.now(clock.withZone(MARRAKECH)).atStartOfDay(MARRAKECH).toInstant();
        return messages.costSince(startOfDay).compareTo(settings.dailyBudgetUsd()) >= 0;
    }

    /** Earlier answered exchanges, oldest first. A guest message without an answer (failure) is skipped. */
    private List<ChatTurn> history(UUID conversationId) {
        List<AssistantMessage> recent = new ArrayList<>(messages.findTop20ByConversationIdOrderByCreatedAtDesc(conversationId));
        Collections.reverse(recent);
        List<ChatTurn> turns = new ArrayList<>();
        for (int i = 0; i + 1 < recent.size(); i++) {
            AssistantMessage guest = recent.get(i);
            AssistantMessage answer = recent.get(i + 1);
            if (guest.getRole() == AssistantRole.GUEST && answer.getRole() == AssistantRole.ASSISTANT
                    && !answer.isFailed() && !answer.getText().isBlank()) {
                turns.add(new ChatTurn("user", guest.getText()));
                turns.add(new ChatTurn("assistant", answer.getText()));
                i++;
            }
        }
        return turns;
    }
}
