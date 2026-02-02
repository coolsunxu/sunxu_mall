package com.example.sunxu_mall.mq.producer.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 消息发送器（内部使用，供 Outbox Dispatcher 调用）
 * <p>
 * 不再作为 MessageProducer Bean 注入，业务代码统一使用 OutboxMessageProducer。
 *
 * @author sunxu
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "kafka")
@RequiredArgsConstructor
public class KafkaMQProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 发送消息到 Kafka（由 Dispatcher 调用）
     *
     * @param topic   Topic
     * @param key     消息Key
     * @param message 消息内容
     */
    public void send(String topic, String key, Object message) {
        log.info("Sending Kafka message to [{}], key: {}, payload: {}", topic, key, message);
        kafkaTemplate.send(topic, key, message);
    }
}
