package cn.jingyier.nail.nailong.common;

public enum BizCode {

    SUCCESS(200, "success"),

    BAD_REQUEST(400, "奶龙挠了挠头，好像没听懂..."),
    NOT_FOUND(404, "奶龙找了半天，什么都没找到..."),
    INTERNAL_ERROR(500, "奶龙被自己绊倒了，请稍后再试..."),

    AI_ERROR(5001, "奶龙的大脑短路了，正在重启中..."),
    DB_ERROR(5002, "奶龙的记忆出现了混乱..."),
    RATE_LIMIT(5003, "奶龙要喘不过气了，请慢一点..."),
    CONCURRENT_TASK(5004, "奶龙正在思考中，请稍等片刻...");

    private final int code;
    private final String message;

    BizCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
