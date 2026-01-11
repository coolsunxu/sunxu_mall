package com.example.sunxu_mall.service.mq.impl;

import cn.hutool.json.JSONUtil;
import com.example.sunxu_mall.dto.mq.MqMessage;
import com.example.sunxu_mall.service.mq.IMqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mq.type", havingValue = "kafka")
public class KafkaMqServiceImpl implements IMqService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.mq.kafka.topic:mall-topic}")
    private String defaultTopic;

    @Override
    public void send(MqMessage message) {
        send(defaultTopic, message);
    }

    @Override
    public void send(String topic, MqMessage message) {
        String msgJson = JSONUtil.toJsonStr(message);
        log.info("Kafka发送消息: topic={}, message={}", topic, msgJson);
        kafkaTemplate.send(topic, message.getBusinessKey(), msgJson);
    }
}
