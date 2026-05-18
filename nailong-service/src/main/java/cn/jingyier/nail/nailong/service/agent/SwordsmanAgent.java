package cn.jingyier.nail.nailong.service.agent;

import cn.jingyier.nail.nailong.animation.AnimationPlayer;
import cn.jingyier.nail.nailong.animation.ChatState;
import cn.jingyier.nail.nailong.common.GlobalExceptionHandler.AiServiceException;
import cn.jingyier.nail.nailong.common.GlobalExceptionHandler.ConcurrentTaskException;
import cn.jingyier.nail.nailong.config.CharacterProperties;
import cn.jingyier.nail.nailong.config.CharacterProperties.CharacterProfile;
import cn.jingyier.nail.nailong.config.CharacterProperties.RecommendationRule;
import cn.jingyier.nail.nailong.config.CharacterProperties.UiTexts;
import cn.jingyier.nail.nailong.entity.Conversation;
import cn.jingyier.nail.nailong.entity.Message;
import cn.jingyier.nail.nailong.entity.vo.MessageVO;
import cn.jingyier.nail.nailong.service.ConversationService;
import cn.jingyier.nail.nailong.service.MessageCacheService;
import cn.jingyier.nail.nailong.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SwordsmanAgent implements CharacterAgent {

    private static final Logger log = LoggerFactory.getLogger(SwordsmanAgent.class);

    private final ChatModel chatModel;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final AnimationPlayer animationPlayer;
    private final ObjectMapper objectMapper;
    private final CharacterProfile profile;
    private final String characterId;
    private final PromptTemplateService templateService;
    private final MessageCacheService messageCacheService;
    private final ConcurrentHashMap<String, Boolean> activeStreams = new ConcurrentHashMap<>();

    public SwordsmanAgent(ChatModel chatModel,
                        ConversationService conversationService,
                        MessageService messageService,
                        AnimationPlayer animationPlayer,
                        ObjectMapper objectMapper,
                        CharacterProperties characterProperties,
                        PromptTemplateService templateService,
                        MessageCacheService messageCacheService,
                        @Value("${nailong.characters.default-id:swordsman}") String defaultCharacterId) {
        this.chatModel = chatModel;
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.animationPlayer = animationPlayer;
        this.objectMapper = objectMapper;
        this.templateService = templateService;
        this.messageCacheService = messageCacheService;
        this.characterId = defaultCharacterId;
        this.profile = characterProperties.getProfile(defaultCharacterId);
    }

    @Override
    public void streamResponse(String sessionKey, String userContent, OutputStream out) {
        if (activeStreams.putIfAbsent(sessionKey, Boolean.TRUE) != null) {
            throw new ConcurrentTaskException(sessionKey);
        }

        try {
            runAgentLoop(sessionKey, userContent, out);
        } catch (IOException e) {
            log.info("Client disconnected for session {}", sessionKey);
        } catch (Exception e) {
            log.error("Agent error for session {}", sessionKey, e);
            try {
                notifyAnimation(ChatState.ERROR);
                writeEvent(out, "error", Map.of(
                        "type", "error",
                        "message", profile.getUiTexts().getErrorMessage(),
                        "characterState", ChatState.ERROR.name().toLowerCase()
                ));
            } catch (IOException ignored) {
            }
        } finally {
            activeStreams.remove(sessionKey);
            notifyAnimation(ChatState.IDLE);
        }
    }

    private void runAgentLoop(String sessionKey, String userContent, OutputStream out) throws IOException {
        Instant startTime = Instant.now();
        UiTexts ui = profile.getUiTexts();

        Conversation conv = conversationService.getOrCreateConversation(sessionKey);
        messageService.saveUserMessage(conv.getId(), userContent);
        conversationService.touchConversation(sessionKey);

        List<org.springframework.ai.chat.messages.Message> messages = buildContext(conv.getId(), userContent);

        notifyAnimation(ChatState.LISTENING);
        writeEvent(out, "thinking", Map.of(
                "type", "thinking",
                "content", ui.getListening(),
                "characterState", ChatState.LISTENING.name().toLowerCase()
        ));

        notifyAnimation(ChatState.THINKING);
        writeEvent(out, "thinking", Map.of(
                "type", "thinking",
                "content", ui.getThinking(),
                "characterState", ChatState.THINKING.name().toLowerCase()
        ));

        StringBuilder fullResponse = new StringBuilder();
        AtomicBoolean hasToolCalls = new AtomicBoolean(false);
        List<Map<String, String>> references = new ArrayList<>();

        try {
            Prompt prompt = new Prompt(messages);
            Flux<ChatResponse> flux = chatModel.stream(prompt);

            flux.doOnNext(resp -> {
                if (resp.getResults() != null) {
                    for (var gen : resp.getResults()) {
                        if (gen.getOutput() != null) {
                            AssistantMessage output = gen.getOutput();

                            if (output.getToolCalls() != null && !output.getToolCalls().isEmpty()) {
                                hasToolCalls.set(true);
                                try {
                                    notifyAnimation(ChatState.THINKING);
                                    writeEvent(out, "thinking", Map.of(
                                            "type", "thinking",
                                            "content", ui.getSearching(),
                                            "characterState", ChatState.THINKING.name().toLowerCase()
                                    ));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            String content = output.getText();
                            if (content != null && !content.isEmpty()) {
                                fullResponse.append(content);
                                try {
                                    notifyAnimation(ChatState.SPEAKING);
                                    writeEvent(out, "text", Map.of(
                                            "type", "text",
                                            "content", content,
                                            "characterState", ChatState.SPEAKING.name().toLowerCase()
                                    ));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }
            }).blockLast(Duration.ofSeconds(120));

        } catch (RuntimeException re) {
            if (re.getCause() instanceof IOException) {
                throw (IOException) re.getCause();
            }
            log.warn("Streaming failed ({}), falling back to blocking call", re.getMessage());
            try {
                Prompt prompt = new Prompt(messages);
                ChatResponse resp = chatModel.call(prompt);
                if (resp != null && resp.getResults() != null) {
                    for (var gen : resp.getResults()) {
                        if (gen.getOutput() != null) {
                            String content = gen.getOutput().getText();
                            if (content != null) {
                                fullResponse.append(content);
                                notifyAnimation(ChatState.SPEAKING);
                                writeEvent(out, "text", Map.of(
                                        "type", "text",
                                        "content", content,
                                        "characterState", ChatState.SPEAKING.name().toLowerCase()
                                ));
                            }
                        }
                    }
                }
            } catch (Exception e2) {
                throw new AiServiceException("AI call failed", e2);
            }
        }

        String finalAnswer = fullResponse.toString();
        if (finalAnswer.isEmpty()) {
            finalAnswer = ui.getFallbackAnswer();
            notifyAnimation(ChatState.SPEAKING);
            writeEvent(out, "text", Map.of(
                    "type", "text",
                    "content", finalAnswer,
                    "characterState", ChatState.SPEAKING.name().toLowerCase()
            ));
        }

        List<String> recommendations = generateRecommendations(userContent);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("firstResponseTimeMs", Duration.between(startTime, Instant.now()).toMillis());
        metadata.put("recommendations", recommendations);
        if (!references.isEmpty()) {
            metadata.put("references", references);
        }

        try {
            String metadataJson = objectMapper.writeValueAsString(metadata);
            MessageVO savedMsg = messageService.saveAssistantMessage(
                    conv.getId(), finalAnswer, metadataJson, "text");
            metadata.put("messageId", savedMsg.getId());

            notifyAnimation(ChatState.IDLE);

            if (!references.isEmpty() || hasToolCalls.get()) {
                writeEvent(out, "reference", Map.of(
                        "type", "reference",
                        "references", references,
                        "characterState", ChatState.IDLE.name().toLowerCase()
                ));
            }

            writeEvent(out, "recommend", Map.of(
                    "type", "recommend",
                    "recommendations", recommendations,
                    "characterState", ChatState.IDLE.name().toLowerCase()
            ));

            writeEvent(out, "complete", Map.of(
                    "type", "complete",
                    "messageId", savedMsg.getId(),
                    "characterState", ChatState.IDLE.name().toLowerCase(),
                    "metadata", metadata
            ));

            conversationService.touchConversation(sessionKey);
        } catch (Exception e) {
            log.error("Failed to persist assistant message", e);
            notifyAnimation(ChatState.ERROR);
            writeEvent(out, "error", Map.of(
                    "type", "error",
                    "message", "保存消息时出了点问题，但答案你已经看到了。",
                    "characterState", ChatState.ERROR.name().toLowerCase()
            ));
        }
    }

    private void notifyAnimation(ChatState state) {
        try {
            animationPlayer.setChatState(state);
        } catch (Exception e) {
            log.debug("Animation notify skipped: {}", e.getMessage());
        }
    }

    private void writeEvent(OutputStream out, String eventName, Object data) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> event = (data instanceof Map) ? new HashMap<>((Map<String, Object>) data) : new HashMap<>();
        event.put("event", eventName);
        String json = objectMapper.writeValueAsString(event);
        synchronized (out) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.write('\n');
            out.flush();
        }
    }

    private List<org.springframework.ai.chat.messages.Message> buildContext(Long convId, String userContent) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        String systemPrompt = loadSystemPrompt();
        messages.add(new SystemMessage(systemPrompt));

        List<Message> history = messageCacheService.getRecent(convId);
        if (history.isEmpty()) {
            history = messageService.getHistoryAsMessages(convId);
            if (!history.isEmpty()) {
                messageCacheService.cacheRecent(convId, history);
            }
        }

        int start = Math.max(0, history.size() - 20);
        for (int i = start; i < history.size(); i++) {
            Message msg = history.get(i);
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        messages.add(new UserMessage(userContent));
        return messages;
    }

    private String loadSystemPrompt() {
        String templatePath = "prompts/" + characterId + "-system.mustache";
        String rendered = templateService.render(templatePath, Map.of(
                "characterName", characterId
        ));
        if (rendered != null) return rendered;
        String yamlPrompt = profile.getSystemPrompt();
        if (yamlPrompt != null && !yamlPrompt.isBlank()) return yamlPrompt;
        throw new IllegalStateException("No system prompt configured for character: " + characterId);
    }

    private List<String> generateRecommendations(String question) {
        List<String> recs = new ArrayList<>();
        String q = question.toLowerCase();

        for (RecommendationRule rule : profile.getRecommendations()) {
            if (rule.getKeywords().isEmpty()) {
                recs.add(rule.getText());
            } else {
                for (String kw : rule.getKeywords()) {
                    if (q.contains(kw.toLowerCase())) {
                        recs.add(rule.getText());
                        break;
                    }
                }
            }
        }

        return recs.subList(0, Math.min(profile.getMaxRecommendations(), recs.size()));
    }
}
