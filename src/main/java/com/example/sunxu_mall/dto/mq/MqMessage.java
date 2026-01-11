package com.example.sunxu_mall.dto.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 消息队列统一消息体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型，用于区分不同的业务逻辑
     * 例如：ORDER_CREATE, USER_LOGIN 等
     */
    private String eventType;

    /**
     * 业务键，用于追踪或幂等处理
     * 例如：订单号、用户ID
     */
    private String businessKey;

    /**
     * 消息内容，建议使用 JSON 字符串或者可序列化的对象
     */
    private Object content;
}
