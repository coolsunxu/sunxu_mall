package com.example.sunxu_mall.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要启用防重复提交校验的接口。
 *
 * @author sunxu
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotency {

    /**
     * 防重窗口时间（秒），小于等于0时使用全局默认配置。
     */
    int ttlSeconds() default 0;
}
