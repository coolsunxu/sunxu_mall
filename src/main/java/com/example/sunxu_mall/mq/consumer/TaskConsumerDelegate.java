package com.example.sunxu_mall.mq.consumer;

import com.example.sunxu_mall.context.AuditContextHolder;
import com.example.sunxu_mall.context.AuditUser;
import com.example.sunxu_mall.entity.common.CommonTaskEntity;
import com.example.sunxu_mall.enums.TaskStatusEnum;
import com.example.sunxu_mall.enums.TaskTypeEnum;
import com.example.sunxu_mall.mapper.common.CommonTaskEntityMapper;
import com.example.sunxu_mall.service.task.IAsyncTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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
    private final IAsyncTask excelExportTask;

    @Async("commonTaskExecutor")
    public void consumeByBizKey(String bizKey) {
        if (Objects.isNull(bizKey)) {
            return;
        }
        log.info("Consuming task by bizKey: {}", bizKey);

        CommonTaskEntity task = commonTaskEntityMapper.selectByBizKey(bizKey);
        if (Objects.isNull(task)) {
            log.warn("Task not found by bizKey: {}", bizKey);
            return;
        }

        if (!Objects.equals(task.getStatus(), TaskStatusEnum.WAITING.getCode())) {
            log.info("Task {} is not in WAITING status, skipping. Current status: {}", bizKey, task.getStatus());
            return;
        }

        try {
            if (Objects.nonNull(task.getCreateUserId()) && Objects.nonNull(task.getCreateUserName())) {
                AuditContextHolder.set(new AuditUser(task.getCreateUserId(), task.getCreateUserName()));
            }

            TaskTypeEnum typeEnum = TaskTypeEnum.getByCode(task.getType());
            if (Objects.isNull(typeEnum)) {
                log.warn("Unknown task type: {}", task.getType());
                return;
            }

            switch (typeEnum) {
                case EXPORT_EXCEL:
                    log.info("Processing Excel export task: id={}, bizKey={}, bizType={}",
                            task.getId(), task.getBizKey(), task.getBizType());
                    excelExportTask.doTask(task);
                    break;
                default:
                    log.debug("No handler for task type: {}", typeEnum);
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to execute task: id={}, bizKey={}", task.getId(), task.getBizKey(), e);
        } finally {
            AuditContextHolder.clear();
        }
    }
}
