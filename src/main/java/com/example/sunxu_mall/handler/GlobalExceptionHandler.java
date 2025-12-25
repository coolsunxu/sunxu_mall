package com.example.sunxu_mall.handler;


import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.util.ApiResult;
import com.example.sunxu_mall.util.ApiResultUtil;
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
     * Handle exceptions uniformly
     *
     * @param e exception
     * @return API response entity
     */
    @ExceptionHandler(Throwable.class)
    public ApiResult handleException(Throwable e) {
        if (e instanceof BusinessException) {
            BusinessException businessException = (BusinessException) e;
            log.info("Business exception occurred:", e);
            return ApiResultUtil.error(businessException.getCode(), businessException.getMessage());
        } else if (e instanceof AccessDeniedException) {
            log.info("Permission exception:", e);
            return ApiResultUtil.error(HttpStatus.FORBIDDEN.value(), "Access denied, please contact system administrator!");
        } else if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException message = (MethodArgumentNotValidException) e;
            BindingResult bindingResult = message.getBindingResult();
            if (bindingResult.hasErrors()) {
                return ApiResultUtil.error(HttpStatus.BAD_REQUEST.value(), bindingResult.getFieldError().getDefaultMessage());
            }
        }
        log.error("System exception occurred:", e);
        return ApiResultUtil.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error, please contact system administrator!");
    }

}
