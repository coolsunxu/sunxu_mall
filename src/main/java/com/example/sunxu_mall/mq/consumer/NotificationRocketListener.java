package com.example.sunxu_mall.mq.consumer;

import com.example.sunxu_mall.dto.mq.MqMessage;
import com.example.sunxu_mall.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author sunxu
 * @description RocketMQ 通知消费者 (迁移自 RocketConsumerJob)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "rocket")
@RocketMQMessageListener(topic = "${app.mq.rocket.notification-topic}",
        consumerGroup = "${app.mq.rocket.notification-consumer-group}")
@RequiredArgsConstructor
public class NotificationRocketListener implements RocketMQListener<String> {

    private final NotificationConsumerDelegate delegate;

    @Override
    public void onMessage(String message) {
        log.info("RocketMQ Notification Consumer received message: {}", message);
        MqMessage mqMessage = JsonUtil.parseObject(message, MqMessage.class);
        if (mqMessage != null) {
            delegate.handleMessage(mqMessage, "ROCKET_NOTIFICATION_TOPIC");
        }
    }
}
