package com.example.sunxu_mall.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/24 19:50
 * @description
 */

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public class BusinessException extends RuntimeException {

    public static final long serialVersionUID = -6735897190745766939L;

    /**
     * 异常码
     */
    private int code;

    /**
     * 具体异常信息
     */
    private String message;

    public BusinessException() {
        super();
    }

    public BusinessException(String message) {
        this.code = HttpStatus.INTERNAL_SERVER_ERROR.value();
        this.message = message;
    }
}

