package cn.jingyier.nail.gateway.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CacheWarmer {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmer.class);
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public CacheWarmer(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedDelay = 60000)
    public void warmCache() {
        redisTemplate.opsForZSet()
                .reverseRangeWithScores("gateway:hot_paths", Range.closed(0L, 4L))
                .collectList()
                .subscribe(entries -> {
                    if (entries.isEmpty()) {
                        log.debug("No hot paths to warm");
                    } else {
                        log.debug("Warming {} hot paths", entries.size());
                    }
                });
    }
}
