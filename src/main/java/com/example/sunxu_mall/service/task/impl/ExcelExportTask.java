package com.example.sunxu_mall.service.task.impl;

import cn.hutool.json.JSONUtil;
import com.example.sunxu_mall.constant.MQConstant;
import com.example.sunxu_mall.dto.BasePageQuery;
import com.example.sunxu_mall.dto.mq.MqMessage;
import com.example.sunxu_mall.dto.websocket.ExportExcelDTO;
import com.example.sunxu_mall.entity.common.CommonNotifyEntity;
import com.example.sunxu_mall.entity.common.CommonTaskEntity;
import com.example.sunxu_mall.enums.ExcelBizTypeEnum;
import com.example.sunxu_mall.enums.TaskStatusEnum;
import com.example.sunxu_mall.enums.TaskTypeEnum;
import com.example.sunxu_mall.errorcode.ErrorCode;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.mq.producer.MessageProducer;
import com.example.sunxu_mall.service.BaseService;
import com.example.sunxu_mall.service.common.CommonNotifyService;
import com.example.sunxu_mall.service.common.CommonTaskService;
import com.example.sunxu_mall.service.task.IAsyncTask;
import com.example.sunxu_mall.util.DateFormatUtil;
import com.example.sunxu_mall.util.FillUserUtil;
import com.example.sunxu_mall.util.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import static com.example.sunxu_mall.constant.NumberConstant.NUMBER_3;

/**
 * @author sunxu
 * @version 1.0
 * @date 2026/1/9 21:21
 * @description
 */

@Slf4j
@Service
public class ExcelExportTask implements IAsyncTask {

    private final CommonTaskService commonTaskService;
    private final MessageProducer messageProducer;
    private final CommonNotifyService commonNotifyService;

    public ExcelExportTask(
            CommonTaskService commonTaskService,
            MessageProducer messageProducer,
            CommonNotifyService commonNotifyService
    ) {
        this.commonTaskService = commonTaskService;
        this.messageProducer = messageProducer;
        this.commonNotifyService = commonNotifyService;
    }

    @Async("exportExecutor")
    @Override
    public void doTask(CommonTaskEntity commonTaskEntity) {
        // 任务逻辑
        doExportExcel(commonTaskEntity);
    }

    private void doExportExcel(CommonTaskEntity commonTaskEntity) {
        // 1. 初始化任务状态
        ExcelBizTypeEnum excelBizTypeEnum = initTaskStatus(commonTaskEntity);

        try {
            // 2. 执行导出逻辑
            String fileUrl = performExport(commonTaskEntity, excelBizTypeEnum);

            // 3. 标记成功
            commonTaskEntity.setFileUrl(fileUrl);
            commonTaskEntity.setStatus(TaskStatusEnum.SUCCESS.getCode());
        } catch (Exception e) {
            // 4. 处理异常
            handleExportError(commonTaskEntity, e);
        }

        // 5. 完成任务（更新状态、发送通知）
        finalizeTask(commonTaskEntity);
    }

    private ExcelBizTypeEnum initTaskStatus(CommonTaskEntity commonTaskEntity) {
        ExcelBizTypeEnum excelBizTypeEnum = ExcelBizTypeEnum.getByCode(commonTaskEntity.getBizType());
        // 任务开始执行时，状态改成执行中
        commonTaskEntity.setStatus(TaskStatusEnum.RUNNING.getCode());
        // 填充操作人员信息
        FillUserUtil.fillUpdateUserInfoFromCreate(commonTaskEntity);
        commonTaskService.update(commonTaskEntity);
        return excelBizTypeEnum;
    }

    private String performExport(CommonTaskEntity commonTaskEntity, ExcelBizTypeEnum excelBizTypeEnum) throws Exception {
        String requestEntity = excelBizTypeEnum.getRequestEntity();
        Class<?> aClass;
        try {
            aClass = Class.forName(requestEntity);
        } catch (ClassNotFoundException e) {
            log.warn("Data export exception, class not found: {}", requestEntity);
            throw new BusinessException(ErrorCode.CLASS_NOT_FOUND_ERROR);
        }

        String requestParam = commonTaskEntity.getRequestParam();
        Object toBean = JSONUtil.toBean(requestParam, aClass);

        String serviceName = excelBizTypeEnum.getServiceName();
        if (ObjectUtils.isEmpty(serviceName)) {
            throw new BusinessException(ErrorCode.EXPORT_CONFIG_ERROR);
        }
        BaseService<?, BasePageQuery> baseService = SpringBeanUtil.getBean(serviceName);

        String exportClassName = excelBizTypeEnum.getExportClassName();
        if (ObjectUtils.isEmpty(exportClassName)) {
            throw new BusinessException(ErrorCode.EXPORT_CONFIG_ERROR);
        }

        String fileName = getFileName(excelBizTypeEnum.getDesc());
        return baseService.export((BasePageQuery) toBean, fileName, exportClassName);
    }

    private void handleExportError(CommonTaskEntity commonTaskEntity, Exception e) {
        log.warn("Data export exception, reason: ", e);
        // 失败次数加1
        commonTaskEntity.setFailureCount((byte) (commonTaskEntity.getFailureCount() + 1));
        // 如果失败次数超过3次，则将状态改成失败，后面不再执行
        if (commonTaskEntity.getFailureCount() >= NUMBER_3) {
            commonTaskEntity.setStatus(TaskStatusEnum.FAIL.getCode());
        }
    }

    private void finalizeTask(CommonTaskEntity commonTaskEntity) {
        // 更新任务状态
        commonTaskService.update(commonTaskEntity);

        // 构建任务结果 JSON
        String taskResultJson = buildTaskResultJson(commonTaskEntity);

        // 发送站内信通知
        CommonNotifyEntity commonNotifyEntity = createNotifyMessage(commonTaskEntity, taskResultJson);
        commonNotifyService.insert(commonNotifyEntity);

        if (!TaskStatusEnum.SUCCESS.getCode().equals(commonTaskEntity.getStatus())) {
            return;
        }

        // 发送通知
        messageProducer.send(
                MQConstant.MALL_COMMON_TASK_TOPIC,
                MQConstant.TAG_NOTIFICATION,
                String.valueOf(commonTaskEntity.getId()),
                MqMessage.builder()
                        .eventType(TaskTypeEnum.EXPORT_EXCEL.getDesc())
                        .businessKey(String.valueOf(commonTaskEntity.getId()))
                        .content(taskResultJson)
                        .build()
        );
    }


    private CommonNotifyEntity createNotifyMessage(CommonTaskEntity commonTaskEntity, String content) {
        return CommonNotifyEntity.builder()
                .title(TaskTypeEnum.EXPORT_EXCEL.getDesc())
                .content(content)
                .toUserId(commonTaskEntity.getCreateUserId())
                .isPush(Boolean.FALSE)
                .type((byte) 1)
                .readStatus((byte) 0)
                .createUserId(commonTaskEntity.getCreateUserId())
                .createUserName(commonTaskEntity.getCreateUserName())
                .createTime(java.time.LocalDateTime.now())
                .isDel(Boolean.FALSE)
                .build();
    }

    private String buildTaskResultJson(CommonTaskEntity commonTaskEntity) {
        ExportExcelDTO dto = ExportExcelDTO.builder()
                .taskId(commonTaskEntity.getId())
                .userId(commonTaskEntity.getCreateUserId())
                .fileName(commonTaskEntity.getName())
                .fileUrl(commonTaskEntity.getFileUrl())
                .build();
        return JSONUtil.toJsonStr(dto);
    }

    private String getFileName(String fileName) {
        return String.format("%s数据_%s", fileName, DateFormatUtil.nowForFile());
    }
}
