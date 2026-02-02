package com.example.sunxu_mall.entity.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MQ Outbox 实体（用于 Kafka/Rocket 事务性消息投递）
 *
 * @author sunxu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqOutboxEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * MQ Topic
     */
    private String topic;

    /**
     * MQ Tag（RocketMQ使用）
     */
    private String tag;

    /**
     * 消息业务Key（用于幂等/追踪）
     */
    private String msgKey;

    /**
     * 消息体JSON
     */
    private String payloadJson;

    /**
     * 消息体类名（用于反序列化）
     */
    private String payloadClass;

    /**
     * 状态：0=NEW待发送，1=SENDING发送中，2=SENT已发送，3=FAILED失败
     */
    private Integer status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 下次重试时间
     */
    private LocalDateTime nextRetryTime;

    /**
     * 最近一次失败原因（截断存储）
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
