package com.example.sunxu_mall.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 用户登录事件
 */
@Getter
public class UserLoginEvent extends ApplicationEvent {
    private final String userId;
    private final String username;
    private final String ip;
    private final LocalDateTime loginTime;

    public UserLoginEvent(Object source, String userId, String username, String ip, LocalDateTime loginTime) {
        super(source);
        this.userId = userId;
        this.username = username;
        this.ip = ip;
        this.loginTime = loginTime;
    }
}
