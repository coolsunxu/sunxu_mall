package com.example.sunxu_mall.mq.consumer;

import com.example.sunxu_mall.dto.mq.MqMessage;
import com.example.sunxu_mall.event.MqEvent;
import com.example.sunxu_mall.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author sunxu
 * @description 本地通知事件消费者 (迁移自 LocalConsumerJob)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class NotificationLocalListener {

    private final NotificationConsumerDelegate delegate;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEvent(MqEvent event) {
        // 过滤掉 CommonTask 触发事件，只处理通知事件（或者通过 Topic 区分）
        // 这里假设通知事件使用 "default-local-topic" 或其他 Topic
        // 兼容 String JSON 与 MqMessage 对象
        Object message = event.getMessage();
        if (message instanceof MqMessage) {
            log.info("Received local notification event: topic={}, message={}", event.getTopic(), message);
            delegate.handleMessage((MqMessage) message, event.getTopic());
        } else if (message instanceof String) {
            MqMessage mqMessage = JsonUtil.parseObject((String) message, MqMessage.class);
            if (mqMessage != null) {
                log.info("Received local notification event: topic={}, message={}", event.getTopic(), mqMessage);
                delegate.handleMessage(mqMessage, event.getTopic());
            }
        }
    }
}
