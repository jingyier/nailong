package cn.jingyier.nail.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class PriorityHeaderFilter implements GlobalFilter, Ordered {

    private static final String PRIORITY_ATTR = "gateway.priority";
    private static final String HEADER_NAME = "X-Priority";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String priority = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (priority != null && !priority.isBlank()) {
            exchange.getAttributes().put(PRIORITY_ATTR, priority.toLowerCase());
        } else {
            exchange.getAttributes().put(PRIORITY_ATTR, "normal");
        }

        exchange.getResponse().getHeaders().add("X-Gateway-Priority",
                (String) exchange.getAttributes().get(PRIORITY_ATTR));

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -85;
    }
}
