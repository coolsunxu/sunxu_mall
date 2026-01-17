package com.example.sunxu_mall.service.common;

import com.example.sunxu_mall.dto.common.CommonTaskRequestDTO;
import com.example.sunxu_mall.entity.common.CommonTaskEntity;
import com.example.sunxu_mall.entity.common.CommonTaskEntityExample;
import com.example.sunxu_mall.enums.ExcelBizTypeEnum;
import com.example.sunxu_mall.enums.TaskStatusEnum;
import com.example.sunxu_mall.enums.TaskTypeEnum;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.mapper.common.CommonTaskEntityMapper;
import com.example.sunxu_mall.util.BeanCopyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.example.sunxu_mall.constant.MQConstant.MALL_COMMON_TASK_TOPIC;
import static com.example.sunxu_mall.constant.MQConstant.TAG_EXCEL_EXPORT;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommonTaskService {

    private final CommonTaskEntityMapper commonTaskEntityMapper;
    private final com.example.sunxu_mall.mq.producer.MessageProducer messageProducer;

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
     * 查询超时未处理的任务 (僵尸任务)
     *
     * @param beforeTime 在此时间之前创建的任务
     * @return 待处理任务列表
     */
    public List<CommonTaskEntity> selectStaleWaitingTasks(LocalDateTime beforeTime) {
        CommonTaskEntityExample example = new CommonTaskEntityExample();
        example.createCriteria()
                .andStatusEqualTo(TaskStatusEnum.WAITING.getCode())
                .andIsDelEqualTo(false)
                .andCreateTimeLessThan(beforeTime);
        // 按创建时间升序，优先处理旧任务
        example.setOrderByClause("create_time asc");
        return commonTaskEntityMapper.selectByExample(example);
    }

    /**
     * 根据请求创建任务（异步削峰用）
     *
     * @param dto 任务请求 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void createTaskFromRequest(CommonTaskRequestDTO dto) {
        if (dto == null || dto.getBizType() == null) {
            log.warn("Invalid task request dto");
            return;
        }

        ExcelBizTypeEnum bizType = dto.getBizType();
        CommonTaskEntity commonTaskEntity = CommonTaskEntity.builder()
                .name(String.format("导出%s数据", bizType.getDesc()))
                .status(TaskStatusEnum.WAITING.getCode())
                .failureCount((byte) 0)
                .type(TaskTypeEnum.EXPORT_EXCEL.getCode())
                .bizType(bizType.getCode())
                .requestParam(dto.getParamJson())
                .createUserId(dto.getUserId())
                .createUserName(dto.getUserName())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDel(false)
                .version(0)
                .build();

        // 调用 insert 方法（包含发送执行消息的逻辑）
        this.insert(commonTaskEntity);
    }

    /**
     * 创建定时任务
     *
     * @param task 定时任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void insert(CommonTaskEntity task) {
        commonTaskEntityMapper.insertSelective(task);
        // 注册事务同步回调，确保事务提交后再发送消息，避免消费者查询不到数据
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    messageProducer.send(
                            MALL_COMMON_TASK_TOPIC,
                            TAG_EXCEL_EXPORT,
                            String.valueOf(task.getId()),
                            task.getId()
                    );
                } catch (Exception e) {
                    log.warn("Failed to send MQ message for task: {}", task.getId(), e);
                }
            }
        });
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
