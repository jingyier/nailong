package cn.jingyier.nail.gateway.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/conversations")
    public Mono<Map<String, Object>> conversationsFallback() {
        return Mono.just(Map.of(
                "code", 503,
                "message", "Service temporarily unavailable, please retry later",
                "data", null,
                "timestamp", System.currentTimeMillis()
        ));
    }
}
