package com.example.sunxu_mall.mq.consumer;

import com.example.sunxu_mall.dto.mq.MqMessage;
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
@RocketMQMessageListener(topic = "${app.mq.rocket.topic:mall-topic}", consumerGroup = "${rocketmq.consumer.group:mall-rocket-group}")
@RequiredArgsConstructor
public class NotificationRocketListener implements RocketMQListener<MqMessage> {

    private final NotificationConsumerDelegate delegate;

    @Override
    public void onMessage(MqMessage message) {
        log.info("RocketMQ Notification Consumer (Default Topic) received message: {}", message);
        delegate.handleMessage(message, "ROCKET_DEFAULT_TOPIC");
    }

    /**
     * 内部类监听第二个 Topic
     */
    @Component
    @ConditionalOnProperty(name = "app.mq.type", havingValue = "rocket")
    @RocketMQMessageListener(topic = "${app.mq.rocket.topic2:mall-topic-2}", consumerGroup = "${rocketmq.consumer.group:mall-rocket-group}-2")
    @RequiredArgsConstructor
    public static class NotificationRocketListener2 implements RocketMQListener<MqMessage> {
        private final NotificationConsumerDelegate delegate;

        @Override
        public void onMessage(MqMessage message) {
            log.info("RocketMQ Notification Consumer (Topic 2) received message: {}", message);
            delegate.handleMessage(message, "ROCKET_TOPIC_2");
        }
    }
}
