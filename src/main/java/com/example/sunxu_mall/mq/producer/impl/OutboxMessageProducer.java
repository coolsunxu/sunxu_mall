package com.example.sunxu_mall.mq.producer.impl;

import com.example.sunxu_mall.entity.mq.MqOutboxEntity;
import com.example.sunxu_mall.enums.OutboxStatusEnum;
import com.example.sunxu_mall.mapper.mq.MqOutboxEntityMapper;
import com.example.sunxu_mall.mq.producer.MessageProducer;
import com.example.sunxu_mall.util.JsonUtil;
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

        // INSERT IGNORE：相同 (topic,tag,msg_key) 不重复插入
        int rows = mqOutboxEntityMapper.insert(outbox);

        if (rows > 0 && Objects.nonNull(outbox.getId()) && outbox.getId() > 0) {
            log.info("Outbox message saved, id={}, topic={}, tag={}, key={}, payloadClass={}",
                    outbox.getId(), topic, tag, key, payloadClass);
        } else {
            log.info("Outbox message already exists (dedup), topic={}, tag={}, key={}", topic, tag, key);
        }
    }

    /**
     * 序列化消息体
     *
     * @param message 消息对象
     * @return JSON 字符串
     */
    private String serializePayload(Object message) {
        if (Objects.isNull(message)) {
            return "null";
        }
        if (message instanceof String) {
            return (String) message;
        }
        if (isPrimitiveWrapper(message)) {
            return String.valueOf(message);
        }
        return JsonUtil.toJsonStr(message);
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
        if (isPrimitiveWrapper(message)) {
            return String.class.getName();
        }
        return message.getClass().getName();
    }

    private boolean isPrimitiveWrapper(Object message) {
        return message instanceof Number
                || message instanceof Boolean
                || message instanceof Character;
    }
}
