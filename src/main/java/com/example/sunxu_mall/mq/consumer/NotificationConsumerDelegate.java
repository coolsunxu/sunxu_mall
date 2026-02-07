package com.example.sunxu_mall.mq.consumer;

import com.example.sunxu_mall.dto.mq.MqMessage;
import com.example.sunxu_mall.dto.websocket.ExportExcelDTO;
import com.example.sunxu_mall.entity.common.CommonNotifyEntity;
import com.example.sunxu_mall.enums.TaskTypeEnum;
import com.example.sunxu_mall.mapper.common.CommonNotifyEntityMapper;
import com.example.sunxu_mall.util.JsonUtil;
import com.example.sunxu_mall.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 通知消息消费逻辑委托类 (WebSocket推送等)
 * <p>
 * 使用数据库状态机 + 原子抢占实现强一致推送，保证同一通知只推送一次
 *
 * @author sunxu
 */
@Slf4j
@Component
public class NotificationConsumerDelegate {

    /**
     * 最大推送重试次数
     */
    @Value("${app.notification.push.max-retry:5}")
    private int maxRetryCount;

    /**
     * 基础退避时间（秒）
     */
    @Value("${app.notification.push.base-backoff-seconds:5}")
    private int baseBackoffSeconds;

    private final CommonNotifyEntityMapper commonNotifyEntityMapper;

    public NotificationConsumerDelegate(CommonNotifyEntityMapper commonNotifyEntityMapper) {
        this.commonNotifyEntityMapper = commonNotifyEntityMapper;
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
        log.info("Notification handling business event: topic={}, type={}, key={}",
                topic, message.getEventType(), message.getBusinessKey());

        // 处理 Excel 导出完成通知
        if (TaskTypeEnum.EXPORT_EXCEL.getDesc().equals(message.getEventType())) {
            handleExcelExportNotification(message);
        }
    }

    /**
     * 处理Excel导出完成通知（强一致推送）
     *
     * @param message 消息内容
     */
    private void handleExcelExportNotification(MqMessage message) {
        log.info("Notification - Processing Excel export completion push...");
        if (StringUtils.isBlank((CharSequence) message.getContent())) {
            return;
        }

        String bizKey = parseBizKey(message.getBusinessKey());
        if (StringUtils.isBlank(bizKey)) {
            return;
        }

        // 1. 原子抢占推送权
        if (!tryLockForPush(bizKey)) {
            log.info("Failed to lock notify for push, bizKey={}, already locked or not ready", bizKey);
            return;
        }

        // 2. 执行推送
        boolean pushSuccess = false;
        String errorMsg = null;
        try {
            pushSuccess = doPush(message, bizKey);
        } catch (Exception e) {
            errorMsg = truncateErrorMsg(e.getMessage());
            log.error("WebSocket push failed, bizKey={}", bizKey, e);
        }

        // 3. 更新推送状态
        if (pushSuccess) {
            markPushSent(bizKey);
        } else {
            handlePushFailure(bizKey, errorMsg);
        }
    }

    /**
     * 解析通知ID
     */
    private String parseBizKey(String businessKey) {
        try {
            return businessKey;
        } catch (Exception e) {
            log.warn("Invalid businessKey format: {}", businessKey, e);
            return null;
        }
    }

    /**
     * 尝试抢占推送权
     *
     * @param notifyId 通知ID
     * @return true=抢占成功，false=抢占失败
     */
    private boolean tryLockForPush(String bizKey) {
        int rows = commonNotifyEntityMapper.tryLockForPushByBizKey(bizKey);
        return rows > 0;
    }

    /**
     * 执行WebSocket推送
     *
     * @param message  消息内容
     * @param notifyId 通知ID
     * @return true=推送成功
     */
    private boolean doPush(MqMessage message, String bizKey) {
        ExportExcelDTO dto = JsonUtil.parseObject(message.getContent().toString(), ExportExcelDTO.class);
        if (Objects.isNull(dto) || Objects.isNull(dto.getUserId())) {
            log.warn("Invalid ExportExcelDTO or userId is null, bizKey={}", bizKey);
            return false;
        }

        Map<String, Object> resp = new HashMap<>(4);
        resp.put("type", "EXPORT_EXCEL");
        resp.put("timestamp", System.currentTimeMillis());
        resp.put("data", dto);

        // WebSocket推送
        WebSocketServer.sendObject(String.valueOf(dto.getUserId()), resp);
        log.info("WebSocket push success, bizKey={}, userId={}", bizKey, dto.getUserId());
        return true;
    }

    /**
     * 标记推送成功
     *
     * @param notifyId 通知ID
     */
    private void markPushSent(String bizKey) {
        try {
            int rows = commonNotifyEntityMapper.markPushSentByBizKey(bizKey);
            if (rows > 0) {
                log.debug("Mark push sent success, bizKey={}", bizKey);
            } else {
                log.warn("Mark push sent failed, bizKey={}, rows=0", bizKey);
            }
        } catch (Exception e) {
            log.error("Mark push sent exception, bizKey={}", bizKey, e);
        }
    }

    /**
     * 处理推送失败：判断是否达到最大重试次数，决定重试或标记死信
     *
     * @param notifyId 通知ID
     * @param errorMsg 错误信息
     */
    private void handlePushFailure(String bizKey, String errorMsg) {
        try {
            // 查询当前重试次数
            CommonNotifyEntity notify = commonNotifyEntityMapper.selectByBizKey(bizKey);
            if (Objects.isNull(notify)) {
                log.warn("Notify not found, bizKey={}", bizKey);
                return;
            }

            int currentRetryCount = Objects.nonNull(notify.getPushRetryCount())
                    ? notify.getPushRetryCount() : 0;

            if (currentRetryCount >= maxRetryCount) {
                // 达到最大重试次数，标记为死信
                commonNotifyEntityMapper.markPushDeadByBizKey(bizKey, errorMsg);
                log.warn("Push reached max retry, marked as DEAD, bizKey={}, retryCount={}",
                        bizKey, currentRetryCount);
            } else {
                // 计算下次重试时间（指数退避）
                LocalDateTime nextRetryTime = calculateNextRetryTime(currentRetryCount);
                commonNotifyEntityMapper.markPushFailedAndRetryByBizKey(bizKey, errorMsg, nextRetryTime);
                log.info("Push failed, scheduled retry, bizKey={}, retryCount={}, nextRetryTime={}",
                        bizKey, currentRetryCount + 1, nextRetryTime);
            }
        } catch (Exception e) {
            log.error("Handle push failure exception, bizKey={}", bizKey, e);
        }
    }

    /**
     * 计算下次重试时间（指数退避）
     *
     * @param currentRetryCount 当前重试次数
     * @return 下次重试时间
     */
    private LocalDateTime calculateNextRetryTime(int currentRetryCount) {
        // 指数退避：baseBackoffSeconds * 2^retryCount，最大不超过1小时
        long backoffSeconds = (long) baseBackoffSeconds * (1L << currentRetryCount);
        backoffSeconds = Math.min(backoffSeconds, 3600L);
        return LocalDateTime.now().plusSeconds(backoffSeconds);
    }

    /**
     * 截断错误信息（最大500字符）
     *
     * @param errorMsg 原始错误信息
     * @return 截断后的错误信息
     */
    private String truncateErrorMsg(String errorMsg) {
        if (StringUtils.isBlank(errorMsg)) {
            return "Unknown error";
        }
        return errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg;
    }
}
