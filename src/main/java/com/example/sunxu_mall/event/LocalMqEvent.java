package com.example.sunxu_mall.event;

import com.example.sunxu_mall.dto.mq.MqMessage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class LocalMqEvent extends ApplicationEvent {

    private final String topic;
    private final MqMessage message;

    public LocalMqEvent(Object source, String topic, MqMessage message) {
        super(source);
        this.topic = topic;
        this.message = message;
    }
}
