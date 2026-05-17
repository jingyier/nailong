package cn.jingyier.nail.nailong.config;

import cn.jingyier.nail.nailong.service.agent.TavilySearchService;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Value("${tavily.api-key}")
    private String tavilyApiKey;

    @Bean
    public TavilySearchService tavilySearchService() {
        return new TavilySearchService(tavilyApiKey);
    }

    @Bean
    public ToolCallback tavilySearchTool(TavilySearchService tavilySearchService) {
        return FunctionToolCallback.builder(
                        "web_search",
                        tavilySearchService
                )
                .description("""
                    搜索互联网获取最新信息。
                    当需要查找实时数据、新闻、或用户询问当前事件时使用此工具。
                    输入 JSON 示例:
                    {
                      "query": "搜索关键词",
                      "searchDepth": "basic",
                      "maxResults": 5
                    }
                    """)
                .inputType(TavilySearchService.SearchRequest.class)
                .build();
    }
}
