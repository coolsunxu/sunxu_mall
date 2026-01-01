package com.example.sunxu_mall.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务状态枚举
 */
@Getter
@AllArgsConstructor
public enum TaskStatusEnum {

    WAITING((byte) 0, "待执行"),
    RUNNING((byte) 1, "执行中"),
    SUCCESS((byte) 2, "成功"),
    FAIL((byte) 3, "失败");

    private final Byte code;
    private final String desc;

    public static TaskStatusEnum getByCode(Byte code) {
        for (TaskStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
