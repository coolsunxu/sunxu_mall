package com.example.sunxu_mall.handler;


import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.util.ApiResult;
import com.example.sunxu_mall.util.ApiResultUtil;
import com.example.sunxu_mall.errorcode.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * Global exception handler
 *
 * @author sunxu
 * @date 2025/12/24 19:56
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResult handleBusinessException(BusinessException e) {
        log.info("Business exception - Code: {}, Message: {}", e.getCode(), e.getMessage());
        return ApiResultUtil.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理权限异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResult handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return ApiResultUtil.error(
            ErrorCode.FORBIDDEN.getCode(),
            ErrorCode.FORBIDDEN.getMessage()
        );
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = "Parameter validation failed";
        if (bindingResult.hasErrors()) {
            errorMessage = bindingResult.getFieldError().getDefaultMessage();
        }
        log.warn("Parameter validation failed: {}", errorMessage);
        return ApiResultUtil.error(
            ErrorCode.PARAMETER_VALIDATION_ERROR.getCode(),
            errorMessage
        );
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return ApiResultUtil.error(
            ErrorCode.PARAMETER_VALIDATION_ERROR.getCode(),
            e.getMessage()
        );
    }

    /**
     * 处理非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public ApiResult handleIllegalStateException(IllegalStateException e) {
        log.warn("Illegal state: {}", e.getMessage());
        return ApiResultUtil.error(
            ErrorCode.OPERATION_FAILED.getCode(),
            "System state error: " + e.getMessage()
        );
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ApiResult handleNullPointerException(NullPointerException e) {
        log.error("Null pointer exception: ", e);
        return ApiResultUtil.error(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            "System internal error occurred"
        );
    }

    /**
     * 处理所有其他未预料的异常（兜底）
     */
    @ExceptionHandler(Throwable.class)
    public ApiResult handleException(Throwable e) {
        // 检查是否是已经处理过的异常类型
        if (e instanceof BusinessException || 
            e instanceof AccessDeniedException || 
            e instanceof MethodArgumentNotValidException ||
            e instanceof IllegalArgumentException ||
            e instanceof IllegalStateException ||
            e instanceof NullPointerException) {
            // 已经处理过，不再重复处理
            return handleException(e);
        }
        
        log.error("Unexpected system exception: ", e);
        return ApiResultUtil.error(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            "Internal server error: " + e.getMessage()
        );
    }


}
