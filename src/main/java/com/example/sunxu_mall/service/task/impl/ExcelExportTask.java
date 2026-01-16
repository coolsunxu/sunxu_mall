package com.example.sunxu_mall.service.task.impl;

import cn.hutool.json.JSONUtil;
import com.example.sunxu_mall.dto.BasePageQuery;
import com.example.sunxu_mall.dto.mq.MqMessage;
import com.example.sunxu_mall.dto.websocket.ExportExcelDTO;
import com.example.sunxu_mall.entity.common.CommonNotifyEntity;
import com.example.sunxu_mall.entity.common.CommonTaskEntity;
import com.example.sunxu_mall.enums.ExcelBizTypeEnum;
import com.example.sunxu_mall.enums.TaskStatusEnum;
import com.example.sunxu_mall.enums.TaskTypeEnum;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.service.BaseService;
import com.example.sunxu_mall.service.common.CommonNotifyService;
import com.example.sunxu_mall.service.common.CommonTaskService;
import com.example.sunxu_mall.mq.producer.MessageProducer;
import com.example.sunxu_mall.service.task.IAsyncTask;
import com.example.sunxu_mall.util.DateFormatUtil;
import com.example.sunxu_mall.util.FillUserUtil;
import com.example.sunxu_mall.util.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void doTask(CommonTaskEntity commonTaskEntity) {
        // 任务逻辑
        doExportExcel(commonTaskEntity);
    }

    private void doExportExcel(CommonTaskEntity commonTaskEntity) {

        // 1 设置初始状态

        // 获取类型
        ExcelBizTypeEnum excelBizTypeEnum = ExcelBizTypeEnum.getByCode(commonTaskEntity.getBizType());
        //任务开始执行时，状态改成执行中
        commonTaskEntity.setStatus(TaskStatusEnum.RUNNING.getCode());
        // 填充操作人员信息
        FillUserUtil.fillUpdateUserInfoFromCreate(commonTaskEntity);

        commonTaskService.update(commonTaskEntity);

        // 执行导出逻辑
        try {
            String requestEntity = excelBizTypeEnum.getRequestEntity();
            Class<?> aClass = null;
            try {
                aClass = Class.forName(requestEntity);
            } catch (ClassNotFoundException e) {
                log.warn("数据导出异常，没有找到:{}", requestEntity);
                throw new BusinessException(String.format("数据导出异常，没有找到:%s", requestEntity));
            }
            String requestParam = commonTaskEntity.getRequestParam();
            Object toBean = JSONUtil.toBean(requestParam, aClass);
            
            String serviceName = excelBizTypeEnum.getServiceName();
            if (ObjectUtils.isEmpty(serviceName)) {
                throw new BusinessException("该业务类型未配置导出服务");
            }
            BaseService baseService = SpringBeanUtil.getBean(serviceName);
            
            String exportClassName = excelBizTypeEnum.getExportClassName();
            if (ObjectUtils.isEmpty(exportClassName)) {
                throw new BusinessException("该业务类型未配置导出实体类");
            }

            String fileName = getFileName(excelBizTypeEnum.getDesc());
            String fileUrl = baseService.export((BasePageQuery) toBean, fileName, exportClassName);
            //执行成功
            commonTaskEntity.setFileUrl(fileUrl);
            commonTaskEntity.setStatus(TaskStatusEnum.SUCCESS.getCode());
        } catch (Exception e) {
            log.warn("数据导出异常，原因：", e);
            //失败次数加1
            commonTaskEntity.setFailureCount((byte) (commonTaskEntity.getFailureCount() + 1));
            //如果失败次数超过3次，则将状态改成失败，后面不再执行
            if (commonTaskEntity.getFailureCount() >= NUMBER_3) {
                commonTaskEntity.setStatus(TaskStatusEnum.FAIL.getCode());
            }
        }

        // 更新任务状态
        commonTaskService.update(commonTaskEntity);
        CommonNotifyEntity commonNotifyEntity = createNotifyMessage(commonTaskEntity);
        commonNotifyService.insert(commonNotifyEntity);

        // 构建消息内容
        String content = "";
        // 只有成功才构建详细DTO
        if (TaskStatusEnum.SUCCESS.getCode().equals(commonTaskEntity.getStatus())) {
            ExportExcelDTO dto = ExportExcelDTO.builder()
                    .taskId(commonTaskEntity.getId())
                    .userId(commonTaskEntity.getCreateUserId())
                    .fileName(commonTaskEntity.getName())
                    .fileUrl(commonTaskEntity.getFileUrl())
                    .build();
            content = JSONUtil.toJsonStr(dto);
        }

        // 发送通知
        messageProducer.send(
                com.example.sunxu_mall.constant.MQConstant.MALL_COMMON_TASK_TOPIC, // 或者定义一个专门的通知 Topic
                "TAG_NOTIFICATION",
                String.valueOf(commonTaskEntity.getId()),
                MqMessage.builder()
                        .eventType(TaskTypeEnum.EXPORT_EXCEL.getDesc())
                        .businessKey(String.valueOf(commonTaskEntity.getId()))
                        .content(content)
                        .build()
        );

    }


    private CommonNotifyEntity createNotifyMessage(CommonTaskEntity commonTaskEntity) {
        CommonNotifyEntity commonNotifyEntity = new CommonNotifyEntity();
        commonNotifyEntity.setTitle(TaskTypeEnum.EXPORT_EXCEL.getDesc());
        commonNotifyEntity.setContent(getContent(commonTaskEntity));
        commonNotifyEntity.setToUserId(commonTaskEntity.getCreateUserId());
        commonNotifyEntity.setIsPush(Boolean.FALSE);
        commonNotifyEntity.setType((byte) 1);
        commonNotifyEntity.setReadStatus((byte) 0);
        commonNotifyEntity.setCreateUserId(commonTaskEntity.getCreateUserId());
        commonNotifyEntity.setCreateUserName(commonTaskEntity.getCreateUserName());
        commonNotifyEntity.setCreateTime(java.time.LocalDateTime.now());
        commonNotifyEntity.setIsDel(Boolean.FALSE);
        return commonNotifyEntity;
    }

    private String getContent(CommonTaskEntity commonTaskEntity) {
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
