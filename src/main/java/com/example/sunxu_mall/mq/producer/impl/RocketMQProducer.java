package com.example.sunxu_mall.mq.producer.impl;

import com.example.sunxu_mall.mq.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * @author sunxu
 * @description RocketMQ 生产者实现
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "rocket")
@RequiredArgsConstructor
public class RocketMQProducer implements MessageProducer {

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public void send(String topic, String tag, String key, Object message) {
        String destination = topic + ":" + tag;
        log.info("Sending RocketMQ message to [{}], key: {}, payload: {}", destination, key, message);
        rocketMQTemplate.syncSend(destination, MessageBuilder.withPayload(message).setHeader("KEYS", key).build());
    }
}
