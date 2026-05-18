package cn.jingyier.nail.nailong.service;

import cn.jingyier.nail.nailong.entity.Message;
import cn.jingyier.nail.nailong.event.MessageSavedEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class MessageCacheService {

    private static final Logger log = LoggerFactory.getLogger(MessageCacheService.class);
    private static final String CACHE_PREFIX = "messages:recent:";
    private static final long CACHE_TTL_SECONDS = 300;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public MessageCacheService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void cacheRecent(Long conversationId, List<Message> messages) {
        try {
            String key = CACHE_PREFIX + conversationId;
            String json = objectMapper.writeValueAsString(messages);
            redis.opsForValue().set(key, json, Duration.ofSeconds(CACHE_TTL_SECONDS));
        } catch (Exception e) {
            log.debug("Failed to cache messages for conv {}: {}", conversationId, e.getMessage());
        }
    }

    public List<Message> getRecent(Long conversationId) {
        try {
            String key = CACHE_PREFIX + conversationId;
            String json = redis.opsForValue().get(key);
            if (json == null || json.isEmpty()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<List<Message>>() {});
        } catch (Exception e) {
            log.debug("Failed to read message cache for conv {}: {}", conversationId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public void invalidate(Long conversationId) {
        try {
            redis.delete(CACHE_PREFIX + conversationId);
        } catch (Exception e) {
            log.debug("Failed to invalidate cache for conv {}", conversationId);
        }
    }

    @EventListener
    public void onMessageSaved(MessageSavedEvent event) {
        invalidate(event.getConversationId());
    }
}