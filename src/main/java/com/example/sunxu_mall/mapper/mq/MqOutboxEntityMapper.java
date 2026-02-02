package com.example.sunxu_mall.mapper.mq;

import com.example.sunxu_mall.entity.mq.MqOutboxEntity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MQ Outbox Mapper
 *
 * @author sunxu
 */
public interface MqOutboxEntityMapper {

    /**
     * 插入Outbox记录
     *
     * @param entity Outbox实体
     * @return 影响行数
     */
    int insert(MqOutboxEntity entity);

    /**
     * 根据ID查询
     *
     * @param id 主键ID
     * @return Outbox实体
     */
    MqOutboxEntity selectByPrimaryKey(Long id);

    /**
     * 查询待发送的消息列表（status=NEW 且 next_retry_time<=now 或 next_retry_time为空）
     *
     * @param limit 最大数量
     * @return 待发送消息列表
     */
    List<MqOutboxEntity> selectPendingMessages(@Param("limit") int limit);

    /**
     * 原子抢占发送权：将 status 从 NEW(0) 改为 SENDING(1)
     *
     * @param id 消息ID
     * @return 影响行数，1=抢占成功，0=抢占失败
     */
    int tryLockForSend(@Param("id") Long id);

    /**
     * 标记发送成功
     *
     * @param id 消息ID
     * @return 影响行数
     */
    int markSent(@Param("id") Long id);

    /**
     * 标记发送失败并设置重试
     *
     * @param id            消息ID
     * @param errorMsg      错误信息
     * @param nextRetryTime 下次重试时间
     * @return 影响行数
     */
    int markFailedAndRetry(@Param("id") Long id,
                           @Param("errorMsg") String errorMsg,
                           @Param("nextRetryTime") LocalDateTime nextRetryTime);

    /**
     * 标记发送失败（终态）
     *
     * @param id       消息ID
     * @param errorMsg 错误信息
     * @return 影响行数
     */
    int markFailed(@Param("id") Long id, @Param("errorMsg") String errorMsg);

    /**
     * 删除已发送的历史消息（清理）
     *
     * @param beforeTime 在此时间之前创建的已发送消息
     * @return 删除的行数
     */
    int deleteOldSentMessages(@Param("beforeTime") LocalDateTime beforeTime);
}
