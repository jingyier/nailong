package cn.jingyier.nail.gateway.filter;

import com.alibaba.fastjson2.JSON;
import org.redisson.api.RedissonClient;
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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class ThreatDetectionFilter implements GlobalFilter, Ordered {

    private static final Pattern[] THREAT_PATTERNS = {
            Pattern.compile("(?i)(\\bSELECT\\b.*\\bFROM\\b|\\bUNION\\s+SELECT\\b|\\bDROP\\s+TABLE\\b|\\bINSERT\\s+INTO\\b|\\bDELETE\\s+FROM\\b|\\bUPDATE\\b.*\\bSET\\b|\\bEXEC\\b.*\\bxp_)"),
            Pattern.compile("(?i)(<script[^>]*>|javascript:|on\\w+\\s*=)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\.\\./|\\.\\.\\\\"),
            Pattern.compile("(?i)(\\bOR\\s+['\"]?1['\"]?\\s*=\\s*['\"]?1['\"]?)")
    };

    private static final Pattern BOT_UA_PATTERN = Pattern.compile(
            "(?i)(scanner|bot|crawler|spider|sqlmap|nmap|nikto|acunetix|burp)",
            Pattern.CASE_INSENSITIVE);

    private final RedissonClient redissonClient;

    @Value("${nailong.gateway.security.blocked-ip-ttl-minutes:30}")
    private int blockedIpTtlMinutes;

    public ThreatDetectionFilter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = getClientIp(exchange);
        String path = exchange.getRequest().getURI().getPath();
        String query = exchange.getRequest().getURI().getQuery();
        String ua = exchange.getRequest().getHeaders().getFirst("User-Agent");

        if (path.startsWith("/actuator/") || path.startsWith("/fallback/")) {
            return chain.filter(exchange);
        }

        if (isIpBlocked(ip)) {
            return reject(exchange, "IP address temporarily blocked", HttpStatus.FORBIDDEN);
        }

        if (ua != null && BOT_UA_PATTERN.matcher(ua).find()) {
            blockIp(ip);
            return reject(exchange, "Suspicious user agent detected", HttpStatus.FORBIDDEN);
        }

        String check = path + (query != null ? "?" + query : "");
        for (Pattern p : THREAT_PATTERNS) {
            if (p.matcher(check).find()) {
                blockIp(ip);
                return reject(exchange, "Malicious request detected", HttpStatus.BAD_REQUEST);
            }
        }

        return chain.filter(exchange);
    }

    private boolean isIpBlocked(String ip) {
        return redissonClient.getBucket("gateway:blocked:ip:" + ip).isExists();
    }

    private void blockIp(String ip) {
        redissonClient.getBucket("gateway:blocked:ip:" + ip)
                .set("blocked", Duration.ofMinutes(blockedIpTtlMinutes));
    }

    private String getClientIp(ServerWebExchange exchange) {
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = JSON.toJSONBytes(Map.of(
                "code", status.value(),
                "message", message,
                "timestamp", System.currentTimeMillis()
        ));
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
