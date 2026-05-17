package cn.jingyier.nail.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Component
public class SanitizationFilter extends AbstractGatewayFilterFactory<SanitizationFilter.Config> {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern SCRIPT_INJECT = Pattern.compile("(?i)(javascript:|on\\w+\\s*=)");

    public SanitizationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            MediaType contentType = exchange.getRequest().getHeaders().getContentType();
            if (contentType == null || !contentType.includes(MediaType.APPLICATION_JSON)) {
                return chain.filter(exchange);
            }

            return DataBufferUtils.join(exchange.getRequest().getBody())
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);

                        String body = new String(bytes, StandardCharsets.UTF_8);
                        String sanitized = sanitize(body);

                        ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
                            @Override
                            public Flux<DataBuffer> getBody() {
                                byte[] sanitizedBytes = sanitized.getBytes(StandardCharsets.UTF_8);
                                DataBuffer buffer = exchange.getResponse().bufferFactory()
                                        .wrap(sanitizedBytes);
                                return Flux.just(buffer);
                            }
                        };

                        return chain.filter(exchange.mutate().request(decorator).build());
                    });
        };
    }

    private String sanitize(String body) {
        String cleaned = HTML_TAG.matcher(body).replaceAll("");
        cleaned = SCRIPT_INJECT.matcher(cleaned).replaceAll("");
        return cleaned;
    }

    public static class Config {
    }
}
