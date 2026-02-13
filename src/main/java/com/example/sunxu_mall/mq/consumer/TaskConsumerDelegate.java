package com.example.sunxu_mall.mq.consumer;

import com.example.sunxu_mall.context.AuditContextHolder;
import com.example.sunxu_mall.context.AuditUser;
import com.example.sunxu_mall.entity.common.CommonTaskEntity;
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

    /**
     * 消费任务：通过原子抢占（WAITING→RUNNING）保证同一任务只被执行一次
     *
     * @param bizKey 任务业务键
     */
    @Async("commonTaskExecutor")
    public void consumeByBizKey(String bizKey) {
        if (Objects.isNull(bizKey)) {
            return;
        }
        log.info("Consuming task by bizKey: {}", bizKey);

        // 原子抢占执行权：CAS 将 status 从 WAITING 改为 RUNNING
        int rows = commonTaskEntityMapper.tryLockForRunByBizKey(bizKey);
        if (rows == 0) {
            log.info("Task {} lock failed (not WAITING or already locked), skipping", bizKey);
            return;
        }

        // 抢占成功，查询完整任务信息
        CommonTaskEntity task = commonTaskEntityMapper.selectByBizKey(bizKey);
        if (Objects.isNull(task)) {
            log.warn("Task not found by bizKey after lock: {}", bizKey);
            return;
        }

        try {
            setAuditContext(task);
            dispatchTask(task);
        } catch (Exception e) {
            log.error("Failed to execute task: id={}, bizKey={}", task.getId(), task.getBizKey(), e);
        } finally {
            AuditContextHolder.clear();
        }
    }

    /**
     * 设置审计上下文
     */
    private void setAuditContext(CommonTaskEntity task) {
        if (Objects.nonNull(task.getCreateUserId()) && Objects.nonNull(task.getCreateUserName())) {
            AuditContextHolder.set(new AuditUser(task.getCreateUserId(), task.getCreateUserName()));
        }
    }

    /**
     * 根据任务类型分发执行
     */
    private void dispatchTask(CommonTaskEntity task) {
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
    }
}
