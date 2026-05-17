package cn.jingyier.nail.gateway.filter;

import com.alibaba.fastjson2.JSON;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class ApiKeyAuthFilter implements GlobalFilter, Ordered {

    @Value("${nailong.gateway.security.api-key:}")
    private String configuredApiKey;

    @Value("${nailong.gateway.security.api-key-header:X-API-Key}")
    private String apiKeyHeader;

    private static final String[] WHITELIST_PATHS = {
            "/actuator/health", "/favicon.ico", "/fallback/"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        for (String whitelist : WHITELIST_PATHS) {
            if (path.startsWith(whitelist)) {
                return chain.filter(exchange);
            }
        }

        String providedKey = exchange.getRequest().getHeaders().getFirst(apiKeyHeader);
        if (!configuredApiKey.equals(providedKey)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] body = JSON.toJSONBytes(Map.of(
                    "code", 401,
                    "message", "Invalid or missing API key",
                    "timestamp", System.currentTimeMillis()
            ));
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
