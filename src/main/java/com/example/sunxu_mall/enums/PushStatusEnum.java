package com.example.sunxu_mall.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知推送状态枚举
 *
 * @author sunxu
 */
@Getter
@AllArgsConstructor
public enum PushStatusEnum {

    /**
     * 待推送
     */
    NEW(0, "待推送"),

    /**
     * 推送中（已被抢占）
     */
    PROCESSING(1, "推送中"),

    /**
     * 推送成功
     */
    SENT(2, "推送成功"),

    /**
     * 推送失败（终态，达到最大重试次数）
     */
    DEAD(3, "推送失败");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     *
     * @param code 状态码
     * @return 枚举值，不存在返回null
     */
    public static PushStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PushStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
