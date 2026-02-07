package com.example.sunxu_mall.mq.consumer;

import com.example.sunxu_mall.dto.common.CommonTaskRequestDTO;
import com.example.sunxu_mall.service.common.CommonTaskService;
import com.example.sunxu_mall.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author sunxu
 * @description Kafka 任务创建消费者 (削峰)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "kafka")
@RequiredArgsConstructor
public class KafkaTaskCreateListener {

    private final CommonTaskService commonTaskService;

    @KafkaListener(topics = "${app.mq.kafka.create-topic}",
            groupId = "${app.mq.kafka.create-consumer-group}")
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("Received Kafka create task request: key={}, value={}", record.key(), record.value());
        try {
            CommonTaskRequestDTO dto = JsonUtil.parseObject(record.value(), CommonTaskRequestDTO.class);
            commonTaskService.createTaskFromRequest(dto);
        } catch (Exception e) {
            log.error("Failed to process create task request: {}", record.value(), e);
        }
    }
}
