package com.example.sunxu_mall.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 邮件业务类型枚举
 */
@Getter
@AllArgsConstructor
public enum EmailTypeEnum {

    REMOTE_LOGIN((byte) 1, "发送异地登录邮件");

    private final Byte code;
    private final String desc;

    public static EmailTypeEnum getByCode(Byte code) {
        for (EmailTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
