package com.example.sunxu_mall.service.common;

import com.example.sunxu_mall.constant.ExportConstant;
import com.example.sunxu_mall.entity.common.CommonNotifyEntity;
import com.example.sunxu_mall.entity.common.CommonNotifyEntityExample;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.mapper.common.CommonNotifyEntityMapper;
import com.example.sunxu_mall.util.BeanCopyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author sunxu
 */

@Slf4j
@Service
public class CommonNotifyService {

    private final CommonNotifyEntityMapper commonNotifyEntityMapper;

    public CommonNotifyService(CommonNotifyEntityMapper commonNotifyEntityMapper) {
        this.commonNotifyEntityMapper = commonNotifyEntityMapper;
    }

    /**
     * 创建通知
     *
     * @param notify 通知
     */
    @Transactional(rollbackFor = Exception.class)
    public void insert(CommonNotifyEntity notify) {
        commonNotifyEntityMapper.insertSelective(notify);
    }

    /**
     * 更新通知
     *
     * @param notify 通知
     */
    @Transactional(rollbackFor = Exception.class)
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void update(CommonNotifyEntity notify) {
        if (Objects.isNull(notify) || Objects.isNull(notify.getId())) {
            throw new BusinessException(400, "Notify ID cannot be null for update");
        }

        Long notifyId = notify.getId();
        CommonNotifyEntity current = commonNotifyEntityMapper.selectByPrimaryKey(notifyId);
        if (current == null) {
            throw new BusinessException(404, "Notify not found");
        }

        Integer oldVersion = current.getVersion();
        BeanCopyUtils.copyNonNullProperties(notify, current);

        current.setVersion(oldVersion);
        current.setUpdateTime(LocalDateTime.now());

        int rows = commonNotifyEntityMapper.updateNotifyWithVersion(current);
        if (rows == 0) {
            log.warn("Optimistic lock failure for notify {}", notifyId);
            throw new OptimisticLockingFailureException("Notify data has been modified, please retry");
        }
    }

    /**
     * 标记通知为已读
     *
     * @param notifyId 通知ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void markAsRead(Long notifyId) {
        if (Objects.isNull(notifyId)) {
            throw new BusinessException(400, "Notify ID cannot be null");
        }

        CommonNotifyEntity current = commonNotifyEntityMapper.selectByPrimaryKey(notifyId);
        if (current == null) {
            throw new BusinessException(404, "Notify not found");
        }

        // 已经是已读状态，直接返回
        if (ExportConstant.READ_STATUS_READ == current.getReadStatus()) {
            log.debug("Notify already read, notifyId={}", notifyId);
            return;
        }

        // 更新为已读状态
        CommonNotifyEntity updateEntity = new CommonNotifyEntity();
        updateEntity.setId(notifyId);
        updateEntity.setReadStatus(ExportConstant.READ_STATUS_READ);
        updateEntity.setVersion(current.getVersion());

        int rows = commonNotifyEntityMapper.updateNotifyWithVersion(updateEntity);
        if (rows == 0) {
            log.warn("Optimistic lock failure when marking notify as read, notifyId={}", notifyId);
            throw new OptimisticLockingFailureException("Notify data has been modified, please retry");
        }

        log.info("Mark notify as read success, notifyId={}", notifyId);
    }

    /**
     * 批量标记当前用户的所有通知为已读
     *
     * @param userId 用户ID
     * @return 更新的记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int markAllAsRead(Long userId) {
        if (Objects.isNull(userId)) {
            throw new BusinessException(400, "User ID cannot be null");
        }

        // 构建更新条件：toUserId = userId AND readStatus = 0
        CommonNotifyEntityExample example = new CommonNotifyEntityExample();
        example.createCriteria()
                .andToUserIdEqualTo(userId)
                .andReadStatusEqualTo(ExportConstant.READ_STATUS_UNREAD);

        // 构建更新内容
        CommonNotifyEntity updateEntity = new CommonNotifyEntity();
        updateEntity.setReadStatus(ExportConstant.READ_STATUS_READ);
        updateEntity.setUpdateTime(LocalDateTime.now());

        int rows = commonNotifyEntityMapper.updateByExampleSelective(updateEntity, example);
        log.info("Mark all notify as read success, userId={}, rows={}", userId, rows);
        return rows;
    }
}
