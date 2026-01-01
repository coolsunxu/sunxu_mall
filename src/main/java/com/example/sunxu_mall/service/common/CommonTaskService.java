package com.example.sunxu_mall.service.common;

import com.example.sunxu_mall.entity.common.CommonTaskEntity;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.mapper.common.CommonTaskEntityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class CommonTaskService {

    private final CommonTaskEntityMapper commonTaskEntityMapper;

    public CommonTaskService(CommonTaskEntityMapper commonTaskEntityMapper) {
        this.commonTaskEntityMapper = commonTaskEntityMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createTask(CommonTaskEntity task) {
        commonTaskEntityMapper.insertSelective(task);
    }

    @Transactional(rollbackFor = Exception.class)
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void updateStatus(Long taskId, Byte newStatus) {
        CommonTaskEntity task = commonTaskEntityMapper.selectByPrimaryKey(taskId);
        if (task == null) {
            throw new BusinessException(404, "Task not found");
        }
        
        task.setStatus(newStatus);
        task.setUpdateTime(LocalDateTime.now());
        
        // 使用乐观锁更新
        int rows = commonTaskEntityMapper.updateStatusWithVersion(task);
        if (rows == 0) {
            log.warn("Optimistic lock failure for task {}", taskId);
            throw new OptimisticLockingFailureException("Task data has been modified, please retry");
        }
    }
}
