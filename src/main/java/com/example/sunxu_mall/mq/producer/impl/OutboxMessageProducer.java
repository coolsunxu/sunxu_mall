package com.example.sunxu_mall.mq.producer.impl;

import cn.hutool.json.JSONUtil;
import com.example.sunxu_mall.entity.mq.MqOutboxEntity;
import com.example.sunxu_mall.enums.OutboxStatusEnum;
import com.example.sunxu_mall.mapper.mq.MqOutboxEntityMapper;
import com.example.sunxu_mall.mq.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Outbox 模式的消息生产者（用于 Kafka/RocketMQ）
 * <p>
 * send() 方法只将消息写入 Outbox 表，由后台投递器（Dispatcher）负责实际发送到 MQ。
 * 这样可以保证消息与业务数据在同一事务内，避免"事务未提交就发消息"的问题。
 *
 * @author sunxu
 */
@Slf4j
@Component
@ConditionalOnExpression("'${app.mq.type:local}' == 'kafka' or '${app.mq.type:local}' == 'rocket'")
@RequiredArgsConstructor
public class OutboxMessageProducer implements MessageProducer {

    private final MqOutboxEntityMapper mqOutboxEntityMapper;

    @Override
    public void send(String topic, String tag, String key, Object message) {
        // 序列化消息
        String payloadJson = serializePayload(message);
        String payloadClass = getPayloadClass(message);

        // 构建 Outbox 记录
        MqOutboxEntity outbox = MqOutboxEntity.builder()
                .topic(topic)
                .tag(tag)
                .msgKey(key)
                .payloadJson(payloadJson)
                .payloadClass(payloadClass)
                .status(OutboxStatusEnum.NEW.getCode())
                .retryCount(0)
                .build();

        // 插入 Outbox 表（在当前事务内）
        mqOutboxEntityMapper.insert(outbox);

        log.info("Outbox message saved, id={}, topic={}, tag={}, key={}, payloadClass={}",
                outbox.getId(), topic, tag, key, payloadClass);
    }

    /**
     * 序列化消息体
     *
     * @param message 消息对象
     * @return JSON 字符串
     */
    private String serializePayload(Object message) {
        if (Objects.isNull(message)) {
            return null;
        }
        if (message instanceof String) {
            return (String) message;
        }
        return JSONUtil.toJsonStr(message);
    }

    /**
     * 获取消息体类名
     *
     * @param message 消息对象
     * @return 类名
     */
    private String getPayloadClass(Object message) {
        if (Objects.isNull(message)) {
            return "null";
        }
        return message.getClass().getName();
    }
}
