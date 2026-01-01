package com.example.sunxu_mall.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 用户登录事件
 */
@Getter
public class UserLoginEvent extends ApplicationEvent {
    private final Long userId;
    private final String username;
    private final String ip;
    private final LocalDateTime loginTime;
    private final String city;

    public UserLoginEvent(Object source, Long userId, String username, String ip, LocalDateTime loginTime, String city) {
        super(source);
        this.userId = userId;
        this.username = username;
        this.ip = ip;
        this.loginTime = loginTime;
        this.city = city;
    }
}
