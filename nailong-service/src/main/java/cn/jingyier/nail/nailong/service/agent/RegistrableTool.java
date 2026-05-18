package cn.jingyier.nail.nailong.service.agent;

import org.springframework.ai.tool.ToolCallback;

public interface RegistrableTool {

    ToolCallback toToolCallback();

    default boolean isEnabled() { return true; }
}
