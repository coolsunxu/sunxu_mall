package com.example.sunxu_mall.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务类型枚举
 */
@Getter
@AllArgsConstructor
public enum TaskTypeEnum {

    EXPORT_EXCEL((byte) 1, "通用Excel数据导出"),
    SEND_EMAIL((byte) 2, "发送邮件");

    private final Byte code;
    private final String desc;

    public static TaskTypeEnum getByCode(Byte code) {
        for (TaskTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
