package com.example.sunxu_mall.mq.producer.impl;

import com.example.sunxu_mall.mq.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * @author sunxu
 * @description Kafka 生产者实现
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "kafka")
@RequiredArgsConstructor
public class KafkaMQProducer implements MessageProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void send(String topic, String tag, String key, Object message) {
        log.info("Sending Kafka message to [{}], key: {}, payload: {}", topic, key, message);
        // Kafka 不直接支持 Tag，这里简单处理只发 Topic，也可以将 Tag 放入 Header
        kafkaTemplate.send(topic, key, message);
    }
}
