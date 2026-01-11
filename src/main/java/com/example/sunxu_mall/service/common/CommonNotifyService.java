package com.example.sunxu_mall.service.common;

import com.example.sunxu_mall.entity.common.CommonNotifyEntity;
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
}
