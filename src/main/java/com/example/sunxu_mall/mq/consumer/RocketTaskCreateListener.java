package com.example.sunxu_mall.mq.consumer;

import com.example.sunxu_mall.constant.MQConstant;
import com.example.sunxu_mall.dto.common.CommonTaskRequestDTO;
import com.example.sunxu_mall.service.common.CommonTaskService;
import com.example.sunxu_mall.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author sunxu
 * @description RocketMQ 任务创建消费者 (削峰)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.type", havingValue = "rocket")
@RocketMQMessageListener(
        topic = MQConstant.MALL_COMMON_TASK_CREATE_TOPIC,
        consumerGroup = "${app.mq.rocket.create-consumer-group}",
        selectorExpression = MQConstant.TAG_EXCEL_EXPORT_CREATE
)
@RequiredArgsConstructor
public class RocketTaskCreateListener implements RocketMQListener<String> {

    private final CommonTaskService commonTaskService;

    @Override
    public void onMessage(String message) {
        log.info("Received RocketMQ create task request: {}", message);
        try {
            CommonTaskRequestDTO dto = JsonUtil.parseObject(message, CommonTaskRequestDTO.class);
            commonTaskService.createTaskFromRequest(dto);
        } catch (Exception e) {
            log.error("Failed to process create task request: {}", message, e);
        }
    }
}
