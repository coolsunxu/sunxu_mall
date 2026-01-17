package com.example.sunxu_mall.mq.consumer;

import cn.hutool.json.JSONUtil;
import com.example.sunxu_mall.dto.mq.MqMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author sunxu
 * @description Kafka 通知消费者 (迁移自 KafkaConsumerJob)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "kafka")
@RequiredArgsConstructor
public class NotificationKafkaListener {

    private final NotificationConsumerDelegate delegate;

    @KafkaListener(topics = {"${app.mq.kafka.topic:mall-topic}", "${app.mq.kafka.topic2:mall-topic-2}"}, groupId = "${spring.kafka.consumer.group-id:mall-group}")
    public void onMessage(ConsumerRecord<?, ?> record) {
        Optional<?> kafkaMessage = Optional.ofNullable(record.value());
        if (kafkaMessage.isPresent()) {
            Object message = kafkaMessage.get();
            log.info("Kafka Notification Consumer received message (Topic={}): {}", record.topic(), message);
            try {
                MqMessage mqMessage = JSONUtil.toBean(message.toString(), MqMessage.class);
                delegate.handleMessage(mqMessage, record.topic());
            } catch (Exception e) {
                log.error("Kafka message parsing failed", e);
            }
        }
    }
}
