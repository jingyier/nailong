package cn.jingyier.nail.nailong.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        return Result.fail(BizCode.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("Validation failed: {}", msg);
        return Result.fail(400, msg);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(ResourceNotFoundException e) {
        return Result.fail(BizCode.NOT_FOUND);
    }

    @ExceptionHandler(AiServiceException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleAiError(AiServiceException e) {
        log.error("AI service error: {}", e.getMessage());
        return Result.fail(BizCode.AI_ERROR);
    }

    @ExceptionHandler(ConcurrentTaskException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleConcurrent(ConcurrentTaskException e) {
        return Result.fail(BizCode.CONCURRENT_TASK);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleGeneric(Exception e) {
        log.error("Unexpected error", e);
        return Result.fail(BizCode.INTERNAL_ERROR);
    }

    // Custom exception classes
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String msg) { super(msg); }
    }

    public static class AiServiceException extends RuntimeException {
        public AiServiceException(String msg) { super(msg); }
        public AiServiceException(String msg, Throwable cause) { super(msg, cause); }
    }

    public static class ConcurrentTaskException extends RuntimeException {
        public ConcurrentTaskException(String sessionKey) {
            super("Conversation " + sessionKey + " already has an active agent task");
        }
    }
}
