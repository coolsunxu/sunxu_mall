package com.example.sunxu_mall.handler;


import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.util.ApiResult;
import com.example.sunxu_mall.util.ApiResultUtil;
import com.example.sunxu_mall.util.SecurityUtil;
import com.example.sunxu_mall.errorcode.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;


/**
 * Global exception handler
 * 统一异常处理，输出结构化日志并在响应中包含 traceId
 *
 * @author sunxu
 * @date 2025/12/24 19:56
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusinessException(BusinessException e) {
        String traceId = getTraceId();
        String username = getCurrentUsernameSafe();
        log.info("[BusinessException] traceId={}, user={}, code={}, message={}", 
                traceId, username, e.getCode(), e.getMessage());
        return buildErrorResult(e.getCode(), e.getMessage(), traceId);
    }

    /**
     * 处理权限异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResult<Void> handleAccessDeniedException(AccessDeniedException e) {
        String traceId = getTraceId();
        String username = getCurrentUsernameSafe();
        log.warn("[AccessDeniedException] traceId={}, user={}, message={}", traceId, username, e.getMessage());
        return buildErrorResult(
            ErrorCode.FORBIDDEN.getCode(),
            ErrorCode.FORBIDDEN.getMessage(),
            traceId
        );
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String traceId = getTraceId();
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = "Parameter validation failed";
        if (bindingResult.hasErrors() && Objects.nonNull(bindingResult.getFieldError())) {
            errorMessage = bindingResult.getFieldError().getDefaultMessage();
        }
        log.warn("[ValidationException] traceId={}, message={}", traceId, errorMessage);
        return buildErrorResult(
            ErrorCode.PARAMETER_VALIDATION_ERROR.getCode(),
            errorMessage,
            traceId
        );
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        String traceId = getTraceId();
        log.warn("[IllegalArgumentException] traceId={}, message={}", traceId, e.getMessage());
        return buildErrorResult(
            ErrorCode.PARAMETER_VALIDATION_ERROR.getCode(),
            e.getMessage(),
            traceId
        );
    }

    /**
     * 处理非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public ApiResult<Void> handleIllegalStateException(IllegalStateException e) {
        String traceId = getTraceId();
        log.warn("[IllegalStateException] traceId={}, message={}", traceId, e.getMessage());
        return buildErrorResult(
            ErrorCode.OPERATION_FAILED.getCode(),
            "System state error: " + e.getMessage(),
            traceId
        );
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ApiResult<Void> handleNullPointerException(NullPointerException e) {
        String traceId = getTraceId();
        String username = getCurrentUsernameSafe();
        log.error("[NullPointerException] traceId={}, user={}", traceId, username, e);
        return buildErrorResult(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            "System internal error occurred",
            traceId
        );
    }

    /**
     * 处理所有其他未预料的异常（兜底）
     */
    @ExceptionHandler(Throwable.class)
    public ApiResult<Void> handleException(Throwable e) {
        String traceId = getTraceId();
        String username = getCurrentUsernameSafe();
        log.error("[UnexpectedException] traceId={}, user={}, type={}", 
                traceId, username, e.getClass().getSimpleName(), e);
        return buildErrorResult(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            "Internal server error",
            traceId
        );
    }

    /**
     * 获取当前 traceId
     */
    private String getTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        return Objects.nonNull(traceId) ? traceId : "unknown";
    }

    /**
     * 安全获取当前用户名（异常处理时不应再抛异常）
     */
    private String getCurrentUsernameSafe() {
        try {
            return SecurityUtil.getCurrentUsername();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    /**
     * 构建包含 traceId 的错误响应
     */
    private ApiResult<Void> buildErrorResult(int code, String message, String traceId) {
        ApiResult<Void> result = ApiResultUtil.error(code, message);
        result.setTraceId(traceId);
        return result;
    }
}
