package com.example.sunxu_mall.mq.consumer;

import com.example.sunxu_mall.constant.MQConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author sunxu
 * @description RocketMQ 任务消费者
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "rocket")
@RocketMQMessageListener(
        topic = "${app.mq.rocket.topic:mall-rocket-topic}",
        consumerGroup = "${rocketmq.consumer.group:mall-rocket-consumer-group}",
        selectorExpression = MQConstant.TAG_EXCEL_EXPORT
)
@RequiredArgsConstructor
public class RocketTaskListener implements RocketMQListener<String> {

    private final TaskConsumerDelegate taskConsumerDelegate;

    @Override
    public void onMessage(String message) {
        log.info("Received RocketMQ message: {}", message);
        try {
            Long taskId = Long.valueOf(message);
            taskConsumerDelegate.consume(taskId);
        } catch (NumberFormatException e) {
            log.error("Invalid task ID format in message: {}", message);
        }
    }
}
