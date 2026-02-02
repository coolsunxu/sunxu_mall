package com.example.sunxu_mall.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MQ Outbox 状态枚举
 *
 * @author sunxu
 */
@Getter
@AllArgsConstructor
public enum OutboxStatusEnum {

    /**
     * 待发送
     */
    NEW(0, "待发送"),

    /**
     * 发送中（已被抢占）
     */
    SENDING(1, "发送中"),

    /**
     * 已发送
     */
    SENT(2, "已发送"),

    /**
     * 发送失败（终态，达到最大重试次数）
     */
    FAILED(3, "发送失败");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     *
     * @param code 状态码
     * @return 枚举值，不存在返回null
     */
    public static OutboxStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OutboxStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
