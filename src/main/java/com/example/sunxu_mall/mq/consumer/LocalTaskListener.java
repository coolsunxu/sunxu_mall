package com.example.sunxu_mall.mq.consumer;

import com.example.sunxu_mall.constant.MQConstant;
import com.example.sunxu_mall.event.MqEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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
    @EventListener(condition = "#event.topic == '" + MQConstant.MALL_COMMON_TASK_TOPIC + "'")
    public void onEvent(MqEvent event) {
        log.info("Received local event: key={}, message={}", event.getKey(), event.getMessage());
        try {
            Long taskId = Long.valueOf(String.valueOf(event.getMessage()));
            taskConsumerDelegate.consume(taskId);
        } catch (NumberFormatException e) {
            log.error("Invalid task ID format in event message: {}", event.getMessage());
        }
    }
}
