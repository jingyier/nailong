package cn.jingyier.nail.nailong.config;

import cn.jingyier.nail.nailong.service.agent.RegistrableTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AgentConfig {

    @Bean
    public List<ToolCallback> toolCallbacks(List<RegistrableTool> tools) {
        return tools.stream()
                .filter(RegistrableTool::isEnabled)
                .map(RegistrableTool::toToolCallback)
                .toList();
    }
}
