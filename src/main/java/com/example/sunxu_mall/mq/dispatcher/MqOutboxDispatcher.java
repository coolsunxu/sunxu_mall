package com.example.sunxu_mall.mq.dispatcher;

import com.example.sunxu_mall.entity.mq.MqOutboxEntity;
import com.example.sunxu_mall.mapper.mq.MqOutboxEntityMapper;
import com.example.sunxu_mall.mq.producer.impl.KafkaMQProducer;
import com.example.sunxu_mall.mq.producer.impl.RocketMQProducer;
import com.example.sunxu_mall.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * MQ Outbox 消息投递器
 * <p>
 * 定时从 Outbox 表拉取待发送消息，通过 CAS 抢占后发送到 Kafka/RocketMQ。
 * 发送成功标记为 SENT，失败则按指数退避重试，达到最大重试次数后标记为 FAILED。
 *
 * @author sunxu
 */
@Slf4j
@Component
@ConditionalOnExpression("'${app.mq.type:local}' == 'kafka' or '${app.mq.type:local}' == 'rocket'")
public class MqOutboxDispatcher {

    /**
     * MQ 类型：kafka 或 rocket
     */
    @Value("${app.mq.type:local}")
    private String mqType;

    /**
     * 每次拉取的最大消息数
     */
    @Value("${app.mq.outbox.batch-size:100}")
    private int batchSize;

    /**
     * 最大重试次数
     */
    @Value("${app.mq.outbox.max-retry:5}")
    private int maxRetry;

    /**
     * 基础退避时间（秒）
     */
    @Value("${app.mq.outbox.base-backoff-seconds:5}")
    private int baseBackoffSeconds;

    private final MqOutboxEntityMapper mqOutboxEntityMapper;

    private final ObjectProvider<KafkaMQProducer> kafkaMQProducerProvider;
    private final ObjectProvider<RocketMQProducer> rocketMQProducerProvider;

    public MqOutboxDispatcher(MqOutboxEntityMapper mqOutboxEntityMapper,
                              ObjectProvider<KafkaMQProducer> kafkaMQProducerProvider,
                              ObjectProvider<RocketMQProducer> rocketMQProducerProvider) {
        this.mqOutboxEntityMapper = mqOutboxEntityMapper;
        this.kafkaMQProducerProvider = kafkaMQProducerProvider;
        this.rocketMQProducerProvider = rocketMQProducerProvider;
    }

    /**
     * 定时投递 Outbox 消息
     */
    @Scheduled(fixedDelayString = "${app.mq.outbox.dispatch-interval-ms:1000}")
    public void dispatch() {
        List<MqOutboxEntity> pendingMessages = mqOutboxEntityMapper.selectPendingMessages(batchSize);
        if (pendingMessages.isEmpty()) {
            return;
        }

        log.debug("Outbox dispatcher found {} pending messages", pendingMessages.size());

        for (MqOutboxEntity outbox : pendingMessages) {
            processOutboxMessage(outbox);
        }
    }

    /**
     * 处理单条 Outbox 消息
     *
     * @param outbox Outbox 实体
     */
    private void processOutboxMessage(MqOutboxEntity outbox) {
        Long outboxId = outbox.getId();

        // 1. CAS 抢占发送权
        if (!tryLockForSend(outboxId)) {
            log.debug("Failed to lock outbox message, id={}, already locked or not ready", outboxId);
            return;
        }

        // 2. 执行发送
        boolean sendSuccess = false;
        String errorMsg = null;
        try {
            sendSuccess = doSend(outbox);
        } catch (Exception e) {
            errorMsg = truncateErrorMsg(e.getMessage());
            log.error("Outbox message send failed, id={}", outboxId, e);
        }

        // 3. 更新状态
        if (sendSuccess) {
            markSent(outboxId);
        } else {
            handleSendFailure(outbox, errorMsg);
        }
    }

    /**
     * 尝试抢占发送权
     *
     * @param outboxId Outbox ID
     * @return true=抢占成功
     */
    private boolean tryLockForSend(Long outboxId) {
        int rows = mqOutboxEntityMapper.tryLockForSend(outboxId);
        return rows > 0;
    }

    /**
     * 执行消息发送
     *
     * @param outbox Outbox 实体
     * @return true=发送成功
     */
    private boolean doSend(MqOutboxEntity outbox) {
        String topic = outbox.getTopic();
        String tag = outbox.getTag();
        String key = outbox.getMsgKey();
        Object payload = deserializePayload(outbox.getPayloadJson(), outbox.getPayloadClass());

        if ("kafka".equalsIgnoreCase(mqType)) {
            return sendToKafka(topic, key, payload);
        } else if ("rocket".equalsIgnoreCase(mqType)) {
            return sendToRocket(topic, tag, key, payload);
        } else {
            log.warn("Unknown MQ type: {}, outbox id={}", mqType, outbox.getId());
            return false;
        }
    }

    /**
     * 发送到 Kafka
     */
    private boolean sendToKafka(String topic, String key, Object payload) {
        KafkaMQProducer kafkaMQProducer = kafkaMQProducerProvider.getIfAvailable();
        if (kafkaMQProducer == null) {
            log.error("KafkaMQProducer is not available");
            return false;
        }
        kafkaMQProducer.send(topic, key, payload);
        return true;
    }

    /**
     * 发送到 RocketMQ
     */
    private boolean sendToRocket(String topic, String tag, String key, Object payload) {
        RocketMQProducer rocketMQProducer = rocketMQProducerProvider.getIfAvailable();
        if (rocketMQProducer == null) {
            log.error("RocketMQProducer is not available");
            return false;
        }
        rocketMQProducer.send(topic, tag, key, payload);
        return true;
    }

    /**
     * 反序列化消息体
     *
     * @param payloadJson  JSON 字符串
     * @param payloadClass 类名
     * @return 消息对象
     */
    private Object deserializePayload(String payloadJson, String payloadClass) {
        if (StringUtils.isBlank(payloadJson) || "null".equals(payloadClass)) {
            return null;
        }

        // 如果是 String 类型，直接返回
        if ("java.lang.String".equals(payloadClass)) {
            return payloadJson;
        }

        // 如果是 Long/Integer 等基本类型包装类，直接返回 JSON 字符串（MQ 会自动处理）
        if (payloadClass.startsWith("java.lang.")) {
            return payloadJson;
        }

        // 尝试反序列化为对象
        try {
            Class<?> clazz = Class.forName(payloadClass);
            return JsonUtil.parseObject(payloadJson, clazz);
        } catch (ClassNotFoundException e) {
            log.warn("Payload class not found: {}, returning raw JSON", payloadClass);
            return payloadJson;
        } catch (Exception e) {
            log.warn("Failed to deserialize payload, class={}, returning raw JSON", payloadClass, e);
            return payloadJson;
        }
    }

    /**
     * 标记发送成功
     *
     * @param outboxId Outbox ID
     */
    private void markSent(Long outboxId) {
        try {
            int rows = mqOutboxEntityMapper.markSent(outboxId);
            if (rows > 0) {
                log.debug("Outbox message sent success, id={}", outboxId);
            } else {
                log.warn("Outbox message mark sent failed, id={}, rows=0", outboxId);
            }
        } catch (Exception e) {
            log.error("Outbox message mark sent exception, id={}", outboxId, e);
        }
    }

    /**
     * 处理发送失败：判断是否达到最大重试次数
     *
     * @param outbox   Outbox 实体
     * @param errorMsg 错误信息
     */
    private void handleSendFailure(MqOutboxEntity outbox, String errorMsg) {
        Long outboxId = outbox.getId();
        int currentRetryCount = Objects.nonNull(outbox.getRetryCount()) ? outbox.getRetryCount() : 0;

        try {
            if (currentRetryCount >= maxRetry) {
                // 达到最大重试次数，标记为失败
                mqOutboxEntityMapper.markFailed(outboxId, errorMsg);
                log.warn("Outbox message reached max retry, marked as FAILED, id={}, retryCount={}",
                        outboxId, currentRetryCount);
            } else {
                // 计算下次重试时间（指数退避）
                LocalDateTime nextRetryTime = calculateNextRetryTime(currentRetryCount);
                mqOutboxEntityMapper.markFailedAndRetry(outboxId, errorMsg, nextRetryTime);
                log.info("Outbox message send failed, scheduled retry, id={}, retryCount={}, nextRetryTime={}",
                        outboxId, currentRetryCount + 1, nextRetryTime);
            }
        } catch (Exception e) {
            log.error("Handle outbox send failure exception, id={}", outboxId, e);
        }
    }

    /**
     * 计算下次重试时间（指数退避）
     *
     * @param currentRetryCount 当前重试次数
     * @return 下次重试时间
     */
    private LocalDateTime calculateNextRetryTime(int currentRetryCount) {
        // 指数退避：baseBackoffSeconds * 2^retryCount，最大不超过1小时
        long backoffSeconds = (long) baseBackoffSeconds * (1L << currentRetryCount);
        backoffSeconds = Math.min(backoffSeconds, 3600L);
        return LocalDateTime.now().plusSeconds(backoffSeconds);
    }

    /**
     * 截断错误信息（最大1000字符）
     *
     * @param errorMsg 原始错误信息
     * @return 截断后的错误信息
     */
    private String truncateErrorMsg(String errorMsg) {
        if (StringUtils.isBlank(errorMsg)) {
            return "Unknown error";
        }
        return errorMsg.length() > 1000 ? errorMsg.substring(0, 1000) : errorMsg;
    }
}
