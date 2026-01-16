package com.example.sunxu_mall.mq.consumer;

import com.example.sunxu_mall.entity.common.CommonTaskEntity;
import com.example.sunxu_mall.enums.TaskStatusEnum;
import com.example.sunxu_mall.enums.TaskTypeEnum;
import com.example.sunxu_mall.mapper.common.CommonTaskEntityMapper;
import com.example.sunxu_mall.service.task.impl.ExcelExportTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author sunxu
 * @description 任务消费逻辑委托类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskConsumerDelegate {

    private final CommonTaskEntityMapper commonTaskEntityMapper;
    private final ExcelExportTask excelExportTask;

    public void consume(Long taskId) {
        if (taskId == null) {
            return;
        }
        log.info("Consuming task: {}", taskId);

        CommonTaskEntity task = commonTaskEntityMapper.selectByPrimaryKey(taskId);
        if (task == null) {
            log.warn("Task not found: {}", taskId);
            return;
        }

        if (!Objects.equals(task.getStatus(), TaskStatusEnum.WAITING.getCode())) {
            log.info("Task {} is not in WAITING status, skipping. Current status: {}", taskId, task.getStatus());
            return;
        }

        try {
            TaskTypeEnum typeEnum = TaskTypeEnum.getByCode(task.getType());
            if (typeEnum == null) {
                log.warn("Unknown task type: {}", task.getType());
                return;
            }

            switch (typeEnum) {
                case EXPORT_EXCEL:
                    log.info("Processing Excel export task: id={}, bizType={}", task.getId(), task.getBizType());
                    excelExportTask.doTask(task);
                    break;
                default:
                    log.debug("No handler for task type: {}", typeEnum);
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to execute task: id={}", task.getId(), e);
        }
    }
}
