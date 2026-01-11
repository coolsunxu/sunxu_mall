package com.example.sunxu_mall.job.mq;

import cn.hutool.json.JSONUtil;
import com.example.sunxu_mall.dto.mq.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "kafka")
public class KafkaConsumerJob {

    /**
     * 监听多个 Topic
     */
    @KafkaListener(topics = {"${app.mq.kafka.topic:mall-topic}", "${app.mq.kafka.topic2:mall-topic-2}"}, groupId = "${spring.kafka.consumer.group-id:mall-group}")
    public void onMessage(ConsumerRecord<?, ?> record) {
        Optional<?> kafkaMessage = Optional.ofNullable(record.value());
        if (kafkaMessage.isPresent()) {
            Object message = kafkaMessage.get();
            log.info("Kafka消费者收到消息 (Topic={}): {}", record.topic(), message);
            try {
                MqMessage mqMessage = JSONUtil.toBean(message.toString(), MqMessage.class);
                handleMessage(mqMessage, record.topic());
            } catch (Exception e) {
                log.error("Kafka消息解析失败", e);
            }
        }
    }

    private void handleMessage(MqMessage message, String topic) {
        log.info("Kafka处理业务事件: topic={}, type={}, key={}", topic, message.getEventType(), message.getBusinessKey());
        // 根据事件类型区分业务
    }
}
