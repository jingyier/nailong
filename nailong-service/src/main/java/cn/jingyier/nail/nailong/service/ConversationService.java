package cn.jingyier.nail.nailong.service;

import cn.jingyier.nail.nailong.common.GlobalExceptionHandler.ResourceNotFoundException;
import cn.jingyier.nail.nailong.entity.Conversation;
import cn.jingyier.nail.nailong.entity.vo.ConversationVO;
import cn.jingyier.nail.nailong.repository.ConversationMapper;
import cn.jingyier.nail.nailong.repository.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ConcurrentHashMap<String, Object> creationLocks = new ConcurrentHashMap<>();

    public ConversationService(ConversationMapper conversationMapper, MessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    public ConversationVO createConversation(String title) {
        Conversation conv = new Conversation();
        conv.setSessionKey(UUID.randomUUID().toString());
        conv.setTitle(title != null && !title.isBlank() ? title : "新对话");
        conv.setStatus("active");
        conversationMapper.insert(conv);
        return toVO(conv);
    }

    public ConversationVO getConversation(String sessionKey) {
        Conversation conv = conversationMapper.selectBySessionKey(sessionKey);
        if (conv == null) {
            throw new ResourceNotFoundException("Conversation not found: " + sessionKey);
        }
        return toVO(conv);
    }

    public List<ConversationVO> listConversations() {
        List<Conversation> convs = conversationMapper.selectActiveConversations();
        return convs.stream().map(this::toVO).toList();
    }

    @Transactional
    public void deleteConversation(String sessionKey) {
        Conversation conv = conversationMapper.selectBySessionKey(sessionKey);
        if (conv == null) {
            throw new ResourceNotFoundException("Conversation not found: " + sessionKey);
        }
        messageMapper.deleteByConversationId(conv.getId());
        conversationMapper.deleteById(conv.getId());
    }

    public Conversation getOrCreateConversation(String sessionKey) {
        Conversation conv = conversationMapper.selectBySessionKey(sessionKey);
        if (conv != null) {
            return conv;
        }
        Object lock = creationLocks.computeIfAbsent(sessionKey, k -> new Object());
        synchronized (lock) {
            conv = conversationMapper.selectBySessionKey(sessionKey);
            if (conv != null) {
                return conv;
            }
            conv = new Conversation();
            conv.setSessionKey(sessionKey);
            conv.setTitle("新对话");
            conv.setStatus("active");
            conversationMapper.insert(conv);
            return conv;
        }
    }

    public void touchConversation(String sessionKey) {
        Conversation conv = conversationMapper.selectBySessionKey(sessionKey);
        if (conv != null) {
            conv.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(conv);
        }
    }

    private ConversationVO toVO(Conversation conv) {
        int messageCount = messageMapper.countByConversationId(conv.getId());
        return new ConversationVO(
                conv.getSessionKey(),
                conv.getTitle(),
                conv.getStatus(),
                conv.getCreatedAt(),
                conv.getUpdatedAt(),
                messageCount
        );
    }
}
