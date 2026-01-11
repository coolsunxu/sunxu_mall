package com.example.sunxu_mall.service.mq.impl;

import com.example.sunxu_mall.dto.mq.MqMessage;
import com.example.sunxu_mall.service.mq.IMqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mq.type", havingValue = "rocket")
public class RocketMqServiceImpl implements IMqService {

    private final RocketMQTemplate rocketMQTemplate;

    @Value("${app.mq.rocket.topic:mall-topic}")
    private String defaultTopic;

    @Override
    public void send(MqMessage message) {
        send(defaultTopic, message);
    }

    @Override
    public void send(String topic, MqMessage message) {
        log.info("RocketMQ发送消息: topic={}, message={}", topic, message);
        // RocketMQTemplate 会自动序列化对象
        rocketMQTemplate.convertAndSend(topic, message);
    }
}
