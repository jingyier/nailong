package cn.jingyier.nail.nailong.service;

import cn.jingyier.nail.nailong.entity.Message;
import cn.jingyier.nail.nailong.entity.vo.MessageVO;
import cn.jingyier.nail.nailong.repository.MessageMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);
    private final MessageMapper messageMapper;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, Object> sequenceLocks = new ConcurrentHashMap<>();

    public MessageService(MessageMapper messageMapper, ObjectMapper objectMapper) {
        this.messageMapper = messageMapper;
        this.objectMapper = objectMapper;
    }

    public MessageVO saveUserMessage(Long conversationId, String content) {
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setRole("user");
        msg.setSequence(nextSequence(conversationId));
        msg.setContent(content);
        msg.setContentType("text");
        messageMapper.insert(msg);
        return toVO(msg);
    }

    public MessageVO saveAssistantMessage(Long conversationId, String content,
                                           String metadata, String contentType) {
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setRole("assistant");
        msg.setSequence(nextSequence(conversationId));
        msg.setContent(content);
        msg.setContentType(contentType != null ? contentType : "text");
        msg.setMetadata(metadata);
        messageMapper.insert(msg);
        return toVO(msg);
    }

    public List<MessageVO> getHistory(Long conversationId) {
        List<Message> messages = messageMapper.selectByConversationId(conversationId);
        return messages.stream().map(this::toVO).toList();
    }

    public List<Message> getHistoryAsMessages(Long conversationId) {
        return messageMapper.selectByConversationId(conversationId);
    }

    public void deleteByConversationId(Long conversationId) {
        messageMapper.deleteByConversationId(conversationId);
    }

    private int nextSequence(Long conversationId) {
        Object lock = sequenceLocks.computeIfAbsent(conversationId, k -> new Object());
        synchronized (lock) {
            return messageMapper.selectMaxSequence(conversationId) + 1;
        }
    }

    private MessageVO toVO(Message msg) {
        Map<String, Object> metadataMap = null;
        if (msg.getMetadata() != null && !msg.getMetadata().isEmpty()) {
            try {
                metadataMap = objectMapper.readValue(msg.getMetadata(), Map.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse message metadata: {}", msg.getMetadata());
                metadataMap = Collections.singletonMap("raw", msg.getMetadata());
            }
        }
        return new MessageVO(
                msg.getId(),
                msg.getRole(),
                msg.getSequence(),
                msg.getContent(),
                msg.getContentType(),
                metadataMap,
                msg.getCreatedAt()
        );
    }
}
