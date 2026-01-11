package com.example.sunxu_mall.job;

import com.example.sunxu_mall.entity.common.CommonTaskEntity;
import com.example.sunxu_mall.enums.ExcelBizTypeEnum;
import com.example.sunxu_mall.enums.TaskTypeEnum;
import com.example.sunxu_mall.service.common.CommonTaskService;
import com.example.sunxu_mall.service.task.impl.ExcelExportTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
    private final ExcelExportTask excelExportTask;

    /**
     * 定时轮询待处理任务
     * cron 表达式从配置文件读取
     */
    @Scheduled(cron = "${mall.task.excel-export-cron:0/10 * * * * ?}")
    public void executeWaitingTasks() {
        // 1. 查询所有待处理 (WAITING) 的任务
        List<CommonTaskEntity> waitingTasks = commonTaskService.selectWaitingTasks();

        if (CollectionUtils.isEmpty(waitingTasks)) {
            return;
        }

        log.info("Found {} waiting tasks", waitingTasks.size());

        for (CommonTaskEntity task : waitingTasks) {
            try {
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
            }
        }
    }
}
