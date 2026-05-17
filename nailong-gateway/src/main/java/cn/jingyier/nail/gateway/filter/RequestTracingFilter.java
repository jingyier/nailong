package cn.jingyier.nail.gateway.filter;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

@Component
public class RequestTracingFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_ATTR = "gateway.requestId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        exchange.getAttributes().put(REQUEST_ID_ATTR, requestId);
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        String finalRequestId = requestId;
        var mutatedExchange = exchange.mutate()
                .request(builder -> builder.header(REQUEST_ID_HEADER, finalRequestId))
                .build();

        MDC.put("requestId", finalRequestId);

        return chain.filter(mutatedExchange)
                .contextWrite(Context.of(REQUEST_ID_ATTR, finalRequestId))
                .doFinally(signal -> MDC.remove("requestId"));
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
