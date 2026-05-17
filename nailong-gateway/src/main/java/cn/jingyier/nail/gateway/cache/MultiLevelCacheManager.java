package cn.jingyier.nail.gateway.cache;

import com.alibaba.fastjson2.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class MultiLevelCacheManager {

    private final Cache<String, CacheEntry> l1Cache;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public MultiLevelCacheManager(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.l1Cache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofSeconds(30))
                .build();
    }

    public Mono<String> get(String key) {
        CacheEntry l1Entry = l1Cache.getIfPresent(key);
        if (l1Entry != null && !l1Entry.isExpired()) {
            return Mono.just(l1Entry.value);
        }

        return redisTemplate.opsForValue().get("gateway:cache:" + key)
                .flatMap(value -> {
                    if (value != null) {
                        l1Cache.put(key, new CacheEntry(value, Duration.ofSeconds(30)));
                        return Mono.just(value);
                    }
                    return Mono.empty();
                });
    }

    public void put(String key, String value, Duration l2Ttl) {
        l1Cache.put(key, new CacheEntry(value, Duration.ofSeconds(30)));
        redisTemplate.opsForValue()
                .set("gateway:cache:" + key, value, l2Ttl)
                .subscribe();
    }

    public void invalidate(String keyPattern) {
        l1Cache.invalidateAll();
        redisTemplate.keys("gateway:cache:" + keyPattern)
                .flatMap(redisTemplate.opsForValue()::delete)
                .subscribe();
    }

    private static class CacheEntry {
        final String value;
        final long expireAt;

        CacheEntry(String value, Duration ttl) {
            this.value = value;
            this.expireAt = System.currentTimeMillis() + ttl.toMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
