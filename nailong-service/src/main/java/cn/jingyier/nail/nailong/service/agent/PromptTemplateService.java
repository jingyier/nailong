package cn.jingyier.nail.nailong.service.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PromptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateService.class);
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private final ResourceLoader resourceLoader;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public PromptTemplateService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String load(String templatePath) {
        return cache.computeIfAbsent(templatePath, path -> {
            Resource resource = resourceLoader.getResource("classpath:" + path);
            try {
                return resource.getContentAsString(StandardCharsets.UTF_8).strip();
            } catch (IOException e) {
                log.warn("Failed to load prompt template: {}", path, e);
                return null;
            }
        });
    }

    public String render(String templatePath, Map<String, String> variables) {
        String template = load(templatePath);
        if (template == null) return null;
        return renderString(template, variables);
    }

    public String renderString(String template, Map<String, String> variables) {
        Matcher m = VAR_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String varName = m.group(1);
            String value = variables.getOrDefault(varName, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public void evictCache(String templatePath) {
        cache.remove(templatePath);
    }
}