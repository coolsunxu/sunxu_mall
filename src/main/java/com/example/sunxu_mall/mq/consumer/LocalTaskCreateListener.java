package com.example.sunxu_mall.mq.consumer;

import cn.hutool.json.JSONUtil;
import com.example.sunxu_mall.constant.MQConstant;
import com.example.sunxu_mall.dto.common.CommonTaskRequestDTO;
import com.example.sunxu_mall.event.MqEvent;
import com.example.sunxu_mall.service.common.CommonTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author sunxu
 * @description Local 任务创建消费者 (削峰)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalTaskCreateListener {

    private final CommonTaskService commonTaskService;

    @Async
    @EventListener(condition = "#event.topic == '" + MQConstant.MALL_COMMON_TASK_CREATE_TOPIC + "'")
    public void onMessage(MqEvent event) {
        log.info("Received Local create task request: {}", event.getMessage());
        try {
            // 假设 message 是 JSON 字符串
            String jsonStr = String.valueOf(event.getMessage());
            CommonTaskRequestDTO dto = JSONUtil.toBean(jsonStr, CommonTaskRequestDTO.class);
            commonTaskService.createTaskFromRequest(dto);
        } catch (Exception e) {
            log.error("Failed to process create task request: {}", event.getMessage(), e);
        }
    }
}
