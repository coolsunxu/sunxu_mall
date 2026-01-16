package com.example.sunxu_mall.mq.producer;

/**
 * @author sunxu
 * @description MQ生产者接口
 */
public interface MessageProducer {

    /**
     * 发送消息
     *
     * @param topic   Topic
     * @param tag     Tag
     * @param key     业务Key (如TaskId)
     * @param message 消息内容
     */
    void send(String topic, String tag, String key, Object message);
}
