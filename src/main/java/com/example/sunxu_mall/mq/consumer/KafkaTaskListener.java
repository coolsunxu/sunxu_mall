package com.example.sunxu_mall.mq.consumer;

import com.example.sunxu_mall.constant.MQConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author sunxu
 * @description Kafka 任务消费者
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "kafka")
@RequiredArgsConstructor
public class KafkaTaskListener {

    private final TaskConsumerDelegate taskConsumerDelegate;

    @KafkaListener(topics = "${app.mq.kafka.topic:mall-kafka-topic}", groupId = "${spring.kafka.consumer.group-id:mall-group}")
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("Received Kafka message: key={}, value={}", record.key(), record.value());
        try {
            // 假设 value 是 taskId
            Long taskId = Long.valueOf(String.valueOf(record.value()));
            taskConsumerDelegate.consume(taskId);
        } catch (NumberFormatException e) {
            log.error("Invalid task ID format in message: {}", record.value());
        }
    }
}
