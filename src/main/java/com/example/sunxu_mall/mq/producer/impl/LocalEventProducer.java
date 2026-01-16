package com.example.sunxu_mall.mq.producer.impl;

import com.example.sunxu_mall.event.MqEvent;
import com.example.sunxu_mall.mq.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * @author sunxu
 * @description 本地事件生产者实现 (Spring Event)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalEventProducer implements MessageProducer {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void send(String topic, String tag, String key, Object message) {
        log.info("Publishing local event, topic: {}, key: {}", topic, key);
        applicationEventPublisher.publishEvent(new MqEvent(this, topic, tag, key, message));
    }
}
