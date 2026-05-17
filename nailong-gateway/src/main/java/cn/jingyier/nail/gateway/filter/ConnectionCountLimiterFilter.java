package cn.jingyier.nail.gateway.filter;

import com.alibaba.fastjson2.JSON;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
public class ConnectionCountLimiterFilter implements GlobalFilter, Ordered {

    private static final int MAX_CONCURRENT_STREAMS_PER_IP = 3;
    private static final String KEY_PREFIX = "gateway:stream:connections:";

    private final RedissonClient redissonClient;

    public ConnectionCountLimiterFilter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!path.matches(".*/api/v1/conversations/[^/]+/messages")
                || !"POST".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
            return chain.filter(exchange);
        }

        String ip = getClientIp(exchange);
        String key = KEY_PREFIX + ip;
        RAtomicLong counter = redissonClient.getAtomicLong(key);

        long current = counter.incrementAndGet();
        counter.expire(Duration.ofSeconds(300));

        if (current > MAX_CONCURRENT_STREAMS_PER_IP) {
            counter.decrementAndGet();
            return reject(exchange);
        }

        return chain.filter(exchange)
                .doFinally(signalType -> counter.decrementAndGet());
    }

    private String getClientIp(ServerWebExchange exchange) {
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = JSON.toJSONBytes(Map.of(
                "code", 429,
                "message", "Too many concurrent streaming connections",
                "timestamp", System.currentTimeMillis()
        ));
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -95;
    }
}
