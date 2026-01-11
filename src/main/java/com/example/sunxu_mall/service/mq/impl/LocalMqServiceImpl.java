package com.example.sunxu_mall.service.mq.impl;

import com.example.sunxu_mall.dto.mq.MqMessage;
import com.example.sunxu_mall.event.LocalMqEvent;
import com.example.sunxu_mall.service.mq.IMqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mq.type", havingValue = "local", matchIfMissing = true)
public class LocalMqServiceImpl implements IMqService {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void send(MqMessage message) {
        send("default-local-topic", message);
    }

    @Override
    public void send(String topic, MqMessage message) {
        log.info("本地事件发布消息: topic={}, message={}", topic, message);
        applicationEventPublisher.publishEvent(new LocalMqEvent(this, topic, message));
    }
}
