package com.example.sunxu_mall.mq.consumer;

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

    @KafkaListener(topics = "${app.mq.kafka.task-topic}",
            groupId = "${app.mq.kafka.task-consumer-group}")
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("Received Kafka message: key={}, value={}", record.key(), record.value());
        String taskBizKey = String.valueOf(record.value());
        taskConsumerDelegate.consumeByBizKey(taskBizKey);
    }
}
