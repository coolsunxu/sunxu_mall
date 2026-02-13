package com.example.sunxu_mall.service.common;

import cn.hutool.core.util.IdUtil;
import com.example.sunxu_mall.context.AuditContextHolder;
import com.example.sunxu_mall.context.AuditUser;
import com.example.sunxu_mall.dto.common.CommonTaskRequestDTO;
import com.example.sunxu_mall.entity.common.CommonTaskEntity;
import com.example.sunxu_mall.entity.common.CommonTaskEntityExample;
import com.example.sunxu_mall.enums.ExcelBizTypeEnum;
import com.example.sunxu_mall.enums.TaskStatusEnum;
import com.example.sunxu_mall.enums.TaskTypeEnum;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.mapper.common.CommonTaskEntityMapper;
import com.example.sunxu_mall.mq.producer.MessageProducer;
import com.example.sunxu_mall.util.BeanCopyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.example.sunxu_mall.constant.MQConstant.MALL_COMMON_TASK_TOPIC;
import static com.example.sunxu_mall.constant.MQConstant.TAG_EXCEL_EXPORT;

/**
 * @author sunxu
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class CommonTaskService {

    private final CommonTaskEntityMapper commonTaskEntityMapper;
    private final MessageProducer messageProducer;

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
     * 根据请求创建任务（异步削峰用，同指纹仅允许一个进行中任务）
     */
    @Transactional(rollbackFor = Exception.class)
    public void createTaskFromRequest(CommonTaskRequestDTO dto) {
        if (Objects.isNull(dto) || Objects.isNull(dto.getBizType())) {
            log.warn("Invalid task request dto");
            return;
        }

        try {
            if (Objects.nonNull(dto.getUserId()) && Objects.nonNull(dto.getUserName())) {
                AuditContextHolder.set(new AuditUser(dto.getUserId(), dto.getUserName()));
            }

            if (isDuplicateInFlightTask(dto.getFingerprint())) {
                return;
            }

            CommonTaskEntity task = buildTaskEntity(dto);
            insertTaskSafely(task);
        } finally {
            AuditContextHolder.clear();
        }
    }

    /**
     * 判断是否存在进行中的同指纹任务（WAITING/RUNNING）
     */
    private boolean isDuplicateInFlightTask(String fingerprint) {
        if (Objects.isNull(fingerprint) || fingerprint.trim().isEmpty()) {
            return false;
        }
        CommonTaskEntity existing = commonTaskEntityMapper.selectInFlightByFingerprint(fingerprint);
        if (Objects.nonNull(existing)) {
            log.info("Duplicate in-flight task detected, fingerprint={}, existingTaskId={}",
                    fingerprint, existing.getId());
            return true;
        }
        return false;
    }

    private CommonTaskEntity buildTaskEntity(CommonTaskRequestDTO dto) {
        ExcelBizTypeEnum bizType = dto.getBizType();
        return CommonTaskEntity.builder()
                .bizKey(IdUtil.getSnowflakeNextIdStr())
                .dedupKey(dto.getDedupKey())
                .fingerprint(dto.getFingerprint())
                .name(String.format("Export %s Data", bizType.getDesc()))
                .status(TaskStatusEnum.WAITING.getCode())
                .failureCount((byte) 0)
                .type(TaskTypeEnum.EXPORT_EXCEL.getCode())
                .bizType(bizType.getCode())
                .requestParam(dto.getParamJson())
                .isDel(false)
                .version(0)
                .build();
    }

    /**
     * 安全插入任务：捕获唯一键冲突异常（DB 兜底幂等）
     *
     * @param task 任务实体
     */
    private void insertTaskSafely(CommonTaskEntity task) {
        try {
            this.insert(task);
        } catch (DuplicateKeyException e) {
            log.info("Task insert duplicate key conflict, dedupKey={}, bizKey={}, skipping",
                    task.getDedupKey(), task.getBizKey());
        }
    }

    /**
     * 创建定时任务
     * <p>
     * 消息发送由统一的 MessageProducer 处理：
     * - local 模式：事件在事务提交后由 @TransactionalEventListener(AFTER_COMMIT) 触发
     * - kafka/rocket 模式：消息写入 Outbox 表（同事务），由 Dispatcher 在事务提交后投递
     *
     * @param task 定时任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void insert(CommonTaskEntity task) {
        commonTaskEntityMapper.insertSelective(task);

        // 直接调用 messageProducer.send()，不再需要手写 afterCommit 回调
        // - local 模式下：publishEvent 触发 TransactionalEventListener，事务提交后执行
        // - kafka/rocket 模式下：写入 Outbox 表（同事务），Dispatcher 负责投递
        messageProducer.send(
                MALL_COMMON_TASK_TOPIC,
                TAG_EXCEL_EXPORT,
                task.getBizKey(),
                task.getBizKey()
        );
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
        if (Objects.isNull(current)) {
            throw new BusinessException(404, "Task not found");
        }

        Integer oldVersion = current.getVersion();
        BeanCopyUtils.copyNonNullProperties(task, current);

        current.setVersion(oldVersion);
        // updateTime由拦截器自动填充

        int rows = commonTaskEntityMapper.updateTaskWithVersion(current);
        if (rows == 0) {
            log.warn("Optimistic lock failure for task {}", taskId);
            throw new OptimisticLockingFailureException("Task data has been modified, please retry");
        }
    }
}
