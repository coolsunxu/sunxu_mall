package com.example.sunxu_mall.util;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON 序列化/反序列化工具（Fastjson2）
 *
 * @author sunxu
 */
@Slf4j
public final class JsonUtil {

    private JsonUtil() {
        // Utility class
    }

    /**
     * 对象转 JSON 字符串
     *
     * @param obj 对象
     * @return JSON 字符串，obj为null返回null
     */
    public static String toJsonStr(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize object to JSON", e);
            return null;
        }
    }

    /**
     * 对象转 JSON 字符串（兼容旧方法名）
     *
     * @param obj 对象
     * @return JSON 字符串
     */
    public static String toJson(Object obj) {
        return toJsonStr(obj);
    }

    /**
     * JSON 字符串转对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   类型
     * @return 反序列化结果，失败返回null
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            log.warn("Failed to parse JSON to object, class={}", clazz, e);
            return null;
        }
    }

    /**
     * JSON 字符串转对象（兼容旧方法名）
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   类型
     * @return 反序列化结果
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return parseObject(json, clazz);
    }
}
