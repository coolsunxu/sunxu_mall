package com.example.sunxu_mall.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * api请求响应实体
 *
 * @author sunxu
 * @date 2025/12/24 19:54
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ApiResult<T> {

    /**
     * 请求成功状态码
     */
    public static final int OK = HttpStatus.OK.value();

    /**
     * 接口返回码
     */
    private int code;

    /**
     * 接口返回信息
     */
    private String message;

    /**
     * 数据
     */
    private T data;
}
