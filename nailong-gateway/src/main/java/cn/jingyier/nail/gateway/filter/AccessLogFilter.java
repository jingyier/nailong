package cn.jingyier.nail.gateway.filter;

import cn.jingyier.nail.gateway.entity.AuditLog;
import cn.jingyier.nail.gateway.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);
    private final AuditLogService auditLogService;

    @Value("${nailong.gateway.audit.enabled:true}")
    private boolean auditEnabled;

    public AccessLogFilter(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        String requestId = (String) exchange.getAttributes().get("gateway.requestId");
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();
        String ip = getClientIp(exchange);
        String ua = exchange.getRequest().getHeaders().getFirst("User-Agent");

        return chain.filter(exchange)
                .doFinally(signal -> {
                    long duration = System.currentTimeMillis() - start;
                    int statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 0;

                    log.info("[{}] {} {} {} {}ms",
                            requestId, method, path, statusCode, duration);

                    if (auditEnabled) {
                        AuditLog entry = new AuditLog();
                        entry.setRequestId(requestId);
                        entry.setClientIp(ip);
                        entry.setMethod(method);
                        entry.setPath(path);
                        entry.setStatusCode(statusCode);
                        entry.setDurationMs(duration);
                        entry.setUserAgent(ua);
                        entry.setCreatedAt(LocalDateTime.now());
                        auditLogService.logAsync(entry);
                    }
                });
    }

    private String getClientIp(ServerWebExchange exchange) {
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
