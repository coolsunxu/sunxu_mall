package com.example.sunxu_mall.service.common;

import com.example.sunxu_mall.entity.common.CommonTaskEntity;
import com.example.sunxu_mall.entity.common.CommonTaskEntityExample;
import com.example.sunxu_mall.enums.TaskStatusEnum;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.mapper.common.CommonTaskEntityMapper;
import com.example.sunxu_mall.util.BeanCopyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class CommonTaskService {

    private final CommonTaskEntityMapper commonTaskEntityMapper;

    public CommonTaskService(CommonTaskEntityMapper commonTaskEntityMapper) {
        this.commonTaskEntityMapper = commonTaskEntityMapper;
    }

    /**
     * 查询所有待处理的任务
     *
     * @return 待处理任务列表
     */
    public List<CommonTaskEntity> selectWaitingTasks() {
        CommonTaskEntityExample example = new CommonTaskEntityExample();
        example.createCriteria().andStatusEqualTo(TaskStatusEnum.WAITING.getCode()).andIsDelEqualTo(false);
        // 按创建时间升序，优先处理旧任务
        example.setOrderByClause("create_time asc");
        return commonTaskEntityMapper.selectByExample(example);
    }

    /**
     * 创建定时任务
     *
     * @param task 定时任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void insert(CommonTaskEntity task) {
        commonTaskEntityMapper.insertSelective(task);
    }

    /**
     * 更新定时任务
     *
     * @param task 定时任务
     */
    @Transactional(rollbackFor = Exception.class)
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void update(CommonTaskEntity task) {
        if (Objects.isNull(task) || Objects.isNull(task.getId())) {
            throw new BusinessException(400, "Task ID cannot be null for update");
        }

        Long taskId = task.getId();
        CommonTaskEntity current = commonTaskEntityMapper.selectByPrimaryKey(taskId);
        if (current == null) {
            throw new BusinessException(404, "Task not found");
        }

        Integer oldVersion = current.getVersion();
        BeanCopyUtils.copyNonNullProperties(task, current);

        current.setVersion(oldVersion);
        current.setUpdateTime(LocalDateTime.now());

        int rows = commonTaskEntityMapper.updateTaskWithVersion(current);
        if (rows == 0) {
            log.warn("Optimistic lock failure for task {}", taskId);
            throw new OptimisticLockingFailureException("Task data has been modified, please retry");
        }
    }
}
