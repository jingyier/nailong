package cn.jingyier.nail.nailong.common;

public enum BizCode {

    SUCCESS(200, "success"),

    BAD_REQUEST(400, "无法理解该请求..."),
    NOT_FOUND(404, "未找到所请求的资源..."),
    INTERNAL_ERROR(500, "服务内部错误，请稍后再试..."),

    AI_ERROR(5001, "AI 服务暂时不可用，正在恢复中..."),
    DB_ERROR(5002, "数据存储异常..."),
    RATE_LIMIT(5003, "请求过于频繁，请稍后再试..."),
    CONCURRENT_TASK(5004, "当前有任务正在处理，请稍等片刻...");

    private final int code;
    private final String message;

    BizCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
