package com.example.sunxu_mall.service.mq;

import com.example.sunxu_mall.dto.mq.MqMessage;

/**
 * 消息队列服务接口
 */
public interface IMqService {
    /**
     * 发送消息（发送到默认 Topic）
     * @param message 消息体
     */
    void send(MqMessage message);

    /**
     * 发送消息到指定 Topic
     * @param topic 目标 Topic
     * @param message 消息体
     */
    void send(String topic, MqMessage message);
}
