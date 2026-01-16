package com.example.sunxu_mall.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author sunxu
 * @description 通用MQ事件 (用于Local模式)
 */
@Getter
public class MqEvent extends ApplicationEvent {
    private final String topic;
    private final String tag;
    private final String key;
    private final Object message;

    public MqEvent(Object source, String topic, String tag, String key, Object message) {
        super(source);
        this.topic = topic;
        this.tag = tag;
        this.key = key;
        this.message = message;
    }
}
