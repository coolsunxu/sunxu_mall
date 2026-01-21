package com.example.sunxu_mall.util;

/**
 * api请求响应实体处理工具类
 *
 * @author sunxu
 * @date 2025/12/24 19:55
 */
public class ApiResultUtil {

    private ApiResultUtil() {
    }

    /**
     * 请求成功
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 接口相应实体
     */
    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(ApiResult.OK);
        result.setData(data);
        return result;
    }

    /**
     * 请求成功
     *
     * @param <T> 数据类型
     * @return 接口相应实体
     */
    public static <T> ApiResult<T> success() {
        return success(null);
    }

    /**
     * 请求成功
     *
     * @param code    返回码
     * @param message 返回信息
     * @param <T>     数据类型
     * @return 接口相应实体
     */
    public static <T> ApiResult<T> error(int code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
