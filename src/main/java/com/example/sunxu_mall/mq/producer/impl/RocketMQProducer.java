package com.example.sunxu_mall.mq.producer.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消息发送器（内部使用，供 Outbox Dispatcher 调用）
 * <p>
 * 不再作为 MessageProducer Bean 注入，业务代码统一使用 OutboxMessageProducer。
 *
 * @author sunxu
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "rocket")
@RequiredArgsConstructor
public class RocketMQProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送消息到 RocketMQ（由 Dispatcher 调用）
     *
     * @param topic   Topic
     * @param tag     Tag
     * @param key     消息Key
     * @param message 消息内容
     */
    public void send(String topic, String tag, String key, Object message) {
        String destination = topic + ":" + tag;
        log.info("Sending RocketMQ message to [{}], key: {}, payload: {}", destination, key, message);
        Object payload = message == null ? "null" : message;
        rocketMQTemplate.syncSend(destination,
                MessageBuilder.withPayload(payload).setHeader("KEYS", key).build());
    }
}
