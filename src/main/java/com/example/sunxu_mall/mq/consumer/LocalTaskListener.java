package com.example.sunxu_mall.mq.consumer;

import com.example.sunxu_mall.constant.MQConstant;
import com.example.sunxu_mall.event.MqEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author sunxu
 * @description 本地事件消费者
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalTaskListener {

    private final TaskConsumerDelegate taskConsumerDelegate;

    @Async("commonTaskExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true,
            condition = "#event.topic == '" + MQConstant.MALL_COMMON_TASK_TOPIC + "'"
    )
    public void onEvent(MqEvent event) {
        log.info("Received local event: key={}, message={}", event.getKey(), event.getMessage());
        String taskBizKey = String.valueOf(event.getMessage());
        taskConsumerDelegate.consumeByBizKey(taskBizKey);
    }
}
