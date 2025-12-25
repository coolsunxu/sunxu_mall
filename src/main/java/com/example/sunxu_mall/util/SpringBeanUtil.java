package com.example.sunxu_mall.util;

import cn.hutool.extra.spring.SpringUtil;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/25 21:18
 * @description
 */


public class SpringBeanUtil {

    /**
     * 根据名称获取bean实例
     *
     * @param name 名称
     * @param <T>  泛型
     * @return bean实例
     */
    public static <T> T getBean(String name) {
        return (T) SpringUtil.getBean(name);
    }

    /**
     * 根据类型获取bean实例
     *
     * @param requiredType 类型
     * @param <T>          泛型
     * @return bean实例
     */
    public static <T> T getBean(Class<T> requiredType) {
        return SpringUtil.getBean(requiredType);
    }
}
