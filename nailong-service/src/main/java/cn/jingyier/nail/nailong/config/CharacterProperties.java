package cn.jingyier.nail.nailong.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "nailong.characters")
@Component
public class CharacterProperties {

    private Map<String, CharacterProfile> profiles = new HashMap<>();

    public Map<String, CharacterProfile> getProfiles() { return profiles; }
    public void setProfiles(Map<String, CharacterProfile> profiles) { this.profiles = profiles; }

    public CharacterProfile getProfile(String id) {
        CharacterProfile profile = profiles.get(id);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown character: " + id);
        }
        return profile;
    }

    public static class CharacterProfile {
        private boolean enabled = true;
        private String personality = "calm";
        private int maxIterations = 10;
        private String systemPrompt;
        private UiTexts uiTexts = new UiTexts();
        private List<RecommendationRule> recommendations = new ArrayList<>();
        private int maxRecommendations = 3;
        private int sleepyTimeoutMinutes = 5;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getPersonality() { return personality; }
        public void setPersonality(String personality) { this.personality = personality; }
        public int getMaxIterations() { return maxIterations; }
        public void setMaxIterations(int maxIterations) { this.maxIterations = maxIterations; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public UiTexts getUiTexts() { return uiTexts; }
        public void setUiTexts(UiTexts uiTexts) { this.uiTexts = uiTexts; }
        public List<RecommendationRule> getRecommendations() { return recommendations; }
        public void setRecommendations(List<RecommendationRule> recommendations) { this.recommendations = recommendations; }
        public int getMaxRecommendations() { return maxRecommendations; }
        public void setMaxRecommendations(int maxRecommendations) { this.maxRecommendations = maxRecommendations; }
        public int getSleepyTimeoutMinutes() { return sleepyTimeoutMinutes; }
        public void setSleepyTimeoutMinutes(int sleepyTimeoutMinutes) { this.sleepyTimeoutMinutes = sleepyTimeoutMinutes; }
    }

    public static class UiTexts {
        private String listening = "听到了。";
        private String thinking = "让我看看...";
        private String searching = "搜索中，稍等。";
        private String errorMessage = "出了点问题，稍后再试。";
        private String fallbackAnswer = "刚才卡了一下，再说一遍？";

        public String getListening() { return listening; }
        public void setListening(String listening) { this.listening = listening; }
        public String getThinking() { return thinking; }
        public void setThinking(String thinking) { this.thinking = thinking; }
        public String getSearching() { return searching; }
        public void setSearching(String searching) { this.searching = searching; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public String getFallbackAnswer() { return fallbackAnswer; }
        public void setFallbackAnswer(String fallbackAnswer) { this.fallbackAnswer = fallbackAnswer; }
    }

    public static class RecommendationRule {
        private List<String> keywords = new ArrayList<>();
        private String text;

        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}
