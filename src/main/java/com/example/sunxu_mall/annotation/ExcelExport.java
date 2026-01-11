package com.example.sunxu_mall.annotation;

import com.example.sunxu_mall.enums.ExcelBizTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author sunxu
 * @version 1.0
 * @date 2026/1/9 20:19
 * @description
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelExport {
    /**
     * excel 导出业务类型
     * @return 业务类型
     */
    ExcelBizTypeEnum value();
}
