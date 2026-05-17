package cn.jingyier.nail.gateway.filter;

import cn.jingyier.nail.gateway.cache.MultiLevelCacheManager;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class ResponseCacheFilter extends AbstractGatewayFilterFactory<ResponseCacheFilter.Config> {

    private final MultiLevelCacheManager cacheManager;

    public ResponseCacheFilter(MultiLevelCacheManager cacheManager) {
        super(Config.class);
        this.cacheManager = cacheManager;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!HttpMethod.GET.equals(exchange.getRequest().getMethod())) {
                return chain.filter(exchange);
            }

            String cacheKey = buildCacheKey(exchange);

            return cacheManager.get(cacheKey)
                    .flatMap(cached -> {
                        byte[] bytes = cached.getBytes(StandardCharsets.UTF_8);
                        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                        exchange.getResponse().getHeaders().set("X-Gateway-Cache", "HIT");
                        return exchange.getResponse().writeWith(Mono.just(buffer));
                    })
                    .switchIfEmpty(chain.filter(captureResponse(exchange, cacheKey, config.ttlSeconds)));
        };
    }

    private ServerWebExchange captureResponse(ServerWebExchange exchange, String cacheKey, long ttlSeconds) {
        ServerHttpResponseDecorator decorated = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return DataBufferUtils.join(Flux.from(body))
                        .flatMap(buffer -> {
                            byte[] bytes = new byte[buffer.readableByteCount()];
                            buffer.read(bytes);
                            DataBufferUtils.release(buffer);

                            if (getStatusCode() != null && getStatusCode().is2xxSuccessful()) {
                                cacheManager.put(cacheKey, new String(bytes, StandardCharsets.UTF_8),
                                        Duration.ofSeconds(ttlSeconds));
                            }

                            DataBuffer out = exchange.getResponse().bufferFactory().wrap(bytes);
                            return super.writeWith(Mono.just(out));
                        });
            }
        };
        return exchange.mutate().response(decorated).build();
    }

    private String buildCacheKey(ServerWebExchange exchange) {
        return exchange.getRequest().getURI().getPath()
                + "?" + exchange.getRequest().getURI().getRawQuery();
    }

    public static class Config {
        long ttlSeconds = 300;
    }
}
