package com.example.sunxu_mall.mq.consumer;

import cn.hutool.json.JSONUtil;
import com.example.sunxu_mall.dto.mq.MqMessage;
import com.example.sunxu_mall.dto.websocket.ExportExcelDTO;
import com.example.sunxu_mall.entity.common.CommonNotifyEntity;
import com.example.sunxu_mall.enums.TaskTypeEnum;
import com.example.sunxu_mall.service.common.CommonNotifyService;
import com.example.sunxu_mall.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 通知消息消费逻辑委托类 (WebSocket推送等)
 *
 * @author sunxu
 */
@Slf4j
@Component
public class NotificationConsumerDelegate {

    private final CommonNotifyService commonNotifyService;

    public NotificationConsumerDelegate(CommonNotifyService commonNotifyService) {
        this.commonNotifyService = commonNotifyService;
    }

    /**
     * 处理通知消息
     *
     * @param message 消息内容
     * @param topic   消息主题
     */
    @Async("notificationExecutor")
    public void handleMessage(MqMessage message, String topic) {
        if (Objects.isNull(message)) {
            return;
        }
        log.info("Notification handling business event: topic={}, type={}, key={}", topic, message.getEventType(), message.getBusinessKey());

        // 处理 Excel 导出完成通知
        if (TaskTypeEnum.EXPORT_EXCEL.getDesc().equals(message.getEventType())) {
            handleExcelExportNotification(message);
        }
    }

    /**
     * 处理Excel导出完成通知
     *
     * @param message 消息内容
     */
    private void handleExcelExportNotification(MqMessage message) {
        log.info("Notification - Processing Excel export completion push...");
        if (StringUtils.isBlank((CharSequence) message.getContent())) {
            return;
        }

        Long notifyId = null;
        try {
            notifyId = Long.valueOf(message.getBusinessKey());
        } catch (NumberFormatException e) {
            log.warn("Invalid businessKey format: {}", message.getBusinessKey());
            return;
        }

        try {
            ExportExcelDTO dto = JSONUtil.toBean(message.getContent().toString(), ExportExcelDTO.class);
            if (Objects.nonNull(dto) && Objects.nonNull(dto.getUserId())) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("type", "EXPORT_EXCEL");
                resp.put("timestamp", System.currentTimeMillis());
                resp.put("data", dto);

                // WebSocket推送
                WebSocketServer.sendObject(String.valueOf(dto.getUserId()), resp);
                log.info("WebSocket push success, notifyId={}, userId={}", notifyId, dto.getUserId());

                // 更新is_push状态为true
                updatePushStatus(notifyId);
            }
        } catch (Exception e) {
            log.error("WebSocket push failed, notifyId={}", notifyId, e);
        }
    }

    /**
     * 更新通知推送状态为已推送
     *
     * @param notifyId 通知ID
     */
    private void updatePushStatus(Long notifyId) {
        try {
            CommonNotifyEntity updateEntity = new CommonNotifyEntity();
            updateEntity.setId(notifyId);
            updateEntity.setIsPush(Boolean.TRUE);
            commonNotifyService.update(updateEntity);
            log.debug("Update notify push status success, notifyId={}", notifyId);
        } catch (Exception e) {
            log.warn("Update notify push status failed, notifyId={}", notifyId, e);
        }
    }
}
