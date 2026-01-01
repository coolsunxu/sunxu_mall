package com.example.sunxu_mall.entity.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 异地登录邮件实体
 *
 * @author sunxu
 * @date 2026/1/1 16:13
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RemoteLoginEmailEntity implements Serializable {

    /**
     * 收件人邮箱
     */
    private String email;

    /**
     * 用户名
     */
    private String username;
    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 登录地点
     */
    private String cityName;

    /**
     * 登录时间
     */
    private String loginTime;


    /**
     * 设备
     */
    private String device;

    /**
     * IP地址
     */
    private String ip;
}
