package com.example.sunxu_mall.job.mq;

import com.example.sunxu_mall.dto.mq.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "rocket")
@RocketMQMessageListener(topic = "${app.mq.rocket.topic:mall-topic}", consumerGroup = "${rocketmq.consumer.group:mall-rocket-group}")
public class RocketConsumerJob implements RocketMQListener<MqMessage> {

    @Override
    public void onMessage(MqMessage message) {
        log.info("RocketMQ消费者(Default Topic)收到消息: {}", message);
        handleMessage(message);
    }

    private void handleMessage(MqMessage message) {
        log.info("RocketMQ处理业务事件: type={}, key={}", message.getEventType(), message.getBusinessKey());
        switch (message.getEventType()) {
            default:
                log.warn("RocketMQ - 未知事件类型: {}", message.getEventType());
        }
    }
    
    /**
     * 内部类监听第二个 Topic
     * RocketMQ 一个类对应一个 Topic Listener，使用内部类是一种组织方式
     */
    @Component
    @ConditionalOnProperty(name = "app.mq.type", havingValue = "rocket")
    @RocketMQMessageListener(topic = "${app.mq.rocket.topic2:mall-topic-2}", consumerGroup = "${rocketmq.consumer.group:mall-rocket-group}-2")
    public static class RocketConsumerJob2 implements RocketMQListener<MqMessage> {
        @Override
        public void onMessage(MqMessage message) {
            log.info("RocketMQ消费者(Topic 2)收到消息: {}", message);
            // 这里可以复用外部类的逻辑或者写新的逻辑
        }
    }
}
