package cn.jingyier.nail.nailong.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {
    // Session memory is managed by MessageService + MySQL.
    // Redis-backed ChatMemory can be added later for caching.
}
