package com.example.sunxu_mall.job.mq;

import cn.hutool.json.JSONUtil;
import com.example.sunxu_mall.dto.mq.MqMessage;
import com.example.sunxu_mall.dto.websocket.ExportExcelDTO;
import com.example.sunxu_mall.enums.TaskTypeEnum;
import com.example.sunxu_mall.event.LocalMqEvent;
import com.example.sunxu_mall.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mq.type", havingValue = "local", matchIfMissing = true)
public class LocalConsumerJob {

    @Async
    @EventListener(LocalMqEvent.class)
    public void handleLocalMqEvent(LocalMqEvent event) {
        try {
            log.info("本地事件监听收到消息(Topic={}): {}", event.getTopic(), event.getMessage());
            handleMessage(event.getMessage(), event.getTopic());
        } catch (Exception e) {
            log.warn("本地事件处理异常", e);
        }
    }

    private void handleMessage(MqMessage message, String topic) {
        log.info("本地事件处理业务事件: topic={}, type={}, key={}", topic, message.getEventType(), message.getBusinessKey());

        if (TaskTypeEnum.EXPORT_EXCEL.getDesc().equals(message.getEventType())) {
            log.info("Local - 处理Excel导出完成...");
            if (StringUtils.isNotBlank((CharSequence) message.getContent())) {
                try {
                    ExportExcelDTO dto = JSONUtil.toBean(message.getContent().toString(), ExportExcelDTO.class);
                    if (dto != null && dto.getUserId() != null) {
                        Map<String, Object> resp = new HashMap<>();
                        resp.put("type", "EXPORT_EXCEL");
                        resp.put("timestamp", System.currentTimeMillis());
                        resp.put("data", dto);

                        WebSocketServer.sendObject(String.valueOf(dto.getUserId()), resp);
                    }
                } catch (Exception e) {
                    log.error("WebSocket推送失败", e);
                }
            }
        }
    }
}
