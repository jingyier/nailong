package cn.jingyier.nail.nailong.service.agent;

public enum CharacterStateMachine {
    IDLE("idle", "待机中"),
    LISTENING("listening", "倾听中"),
    THINKING("thinking", "思考中"),
    SPEAKING("speaking", "说话中"),
    LAUGHING("laughing", "开心"),
    SLEEPY("sleepy", "犯困");

    private final String code;
    private final String displayName;

    CharacterStateMachine(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
}
