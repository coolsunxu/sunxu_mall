package com.example.sunxu_mall.job;

import com.example.sunxu_mall.context.AuditContextHolder;
import com.example.sunxu_mall.context.AuditUser;
import com.example.sunxu_mall.entity.common.CommonTaskEntity;
import com.example.sunxu_mall.enums.ExcelBizTypeEnum;
import com.example.sunxu_mall.enums.TaskTypeEnum;
import com.example.sunxu_mall.service.common.CommonTaskService;
import com.example.sunxu_mall.service.task.IAsyncTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * @author sunxu
 * @description 通用任务轮询 Job
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonTaskJob {

    private final CommonTaskService commonTaskService;
    private final IAsyncTask excelExportTask;

    /**
     * 定时轮询待处理任务 (兜底机制)
     * cron 表达式从配置文件读取
     */
    @Scheduled(cron = "${mall.task.excel-export-cron:0 0/5 * * * ?}")
    public void executeWaitingTasks() {
        // 1. 查询所有超时未处理 (WAITING) 的任务 (10分钟前)
        // 避免与 MQ 消费者产生竞争
        LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(10);
        List<CommonTaskEntity> waitingTasks = commonTaskService.selectStaleWaitingTasks(thresholdTime);

        if (CollectionUtils.isEmpty(waitingTasks)) {
            return;
        }

        log.info("Found {} waiting tasks", waitingTasks.size());

        for (CommonTaskEntity task : waitingTasks) {
            try {
                if (Objects.nonNull(task.getCreateUserId()) && Objects.nonNull(task.getCreateUserName())) {
                    AuditContextHolder.set(new AuditUser(task.getCreateUserId(), task.getCreateUserName()));
                }

                switch (Objects.requireNonNull(TaskTypeEnum.getByCode(task.getType()))) {
                    case EXPORT_EXCEL:
                        log.info("Processing Excel export task: id={}, bizType={}", task.getId(), task.getBizType());
                        excelExportTask.doTask(task);
                        break;
                    default:
                        log.debug("Skipping non-excel task: id={}, bizType={}", task.getId(), task.getBizType());
                        break;
                }
            } catch (Exception e) {
                log.warn("Failed to execute task: id={}", task.getId(), e);
            } finally {
                AuditContextHolder.clear();
            }
        }
    }
}
