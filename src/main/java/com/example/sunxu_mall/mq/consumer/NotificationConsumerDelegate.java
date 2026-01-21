package com.example.sunxu_mall.mq.consumer;

import cn.hutool.json.JSONUtil;
import com.example.sunxu_mall.dto.mq.MqMessage;
import com.example.sunxu_mall.dto.websocket.ExportExcelDTO;
import com.example.sunxu_mall.enums.TaskTypeEnum;
import com.example.sunxu_mall.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author sunxu
 * @description 通知消息消费逻辑委托类 (WebSocket推送等)
 */
@Slf4j
@Component
public class NotificationConsumerDelegate {

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

    private void handleExcelExportNotification(MqMessage message) {
        log.info("Notification - Processing Excel export completion push...");
        if (StringUtils.isBlank((CharSequence) message.getContent())) {
            return;
        }

        try {
            ExportExcelDTO dto = JSONUtil.toBean(message.getContent().toString(), ExportExcelDTO.class);
            if (Objects.nonNull(dto) && Objects.nonNull(dto.getUserId())) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("type", "EXPORT_EXCEL");
                resp.put("timestamp", System.currentTimeMillis());
                resp.put("data", dto);

                WebSocketServer.sendObject(String.valueOf(dto.getUserId()), resp);
            }
        } catch (Exception e) {
            log.error("WebSocket push failed", e);
        }
    }
}
