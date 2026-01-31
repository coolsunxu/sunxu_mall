package com.example.sunxu_mall.service.mall;

import com.example.sunxu_mall.dto.mall.AttributeValueQueryDTO;
import com.example.sunxu_mall.dto.mall.CreateAttributeValueDTO;
import com.example.sunxu_mall.dto.mall.UpdateAttributeValueDTO;
import com.example.sunxu_mall.entity.mall.MallAttributeEntity;
import com.example.sunxu_mall.entity.mall.MallAttributeValueEntity;
import com.example.sunxu_mall.entity.mall.MallAttributeValueEntityExample;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.mapper.mall.MallAttributeEntityMapper;
import com.example.sunxu_mall.mapper.mall.MallAttributeValueEntityMapper;
import com.example.sunxu_mall.model.ResponseCursorEntity;
import com.example.sunxu_mall.service.BaseService;
import com.example.sunxu_mall.util.BeanCopyUtils;
import com.example.sunxu_mall.vo.mall.AttributeValueVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.example.sunxu_mall.errorcode.ErrorCode.PARAMETER_MISSING;

/**
 * 属性值服务类
 * 采用 MyBatis 拦截器自动填充审计字段，乐观锁实现并发控制
 *
 * @author sunxu
 * @version 1.0
 */
@Slf4j
@Service
public class AttributeValueService extends BaseService<AttributeValueVO, AttributeValueQueryDTO> {


    private final MallAttributeValueEntityMapper attributeValueEntityMapper;


    private final MallAttributeEntityMapper attributeEntityMapper;

    public AttributeValueService(
            MallAttributeValueEntityMapper attributeValueEntityMapper,
            MallAttributeEntityMapper attributeEntityMapper
    ) {
        this.attributeValueEntityMapper = attributeValueEntityMapper;
        this.attributeEntityMapper = attributeEntityMapper;
    }


    @Override
    public ResponseCursorEntity<AttributeValueVO> searchByBidirectionalCursor(AttributeValueQueryDTO queryDTO) {
        return super.searchByBidirectionalCursor(queryDTO);
    }

    @Override
    protected List<AttributeValueVO> selectListWithLimit(AttributeValueQueryDTO query, int limit) {
        return attributeValueEntityMapper.selectListWithLimit(
                query.getAttributeId(),
                query.getValue(),
                limit
        );
    }

    @Override
    protected List<AttributeValueVO> selectListByCursorWithLimit(AttributeValueQueryDTO query, Long cursorId, int limit) {
        return attributeValueEntityMapper.selectByCursorWithLimit(
                query.getAttributeId(),
                query.getValue(),
                cursorId,
                limit
        );
    }

    @Override
    protected Long extractEntityId(AttributeValueVO entity) {
        return Objects.isNull(entity) ? null : entity.getId();
    }


    /**
     * 插入属性值
     * 审计字段由 MyBatis 拦截器自动填充
     *
     * @param createDTO 创建属性值 DTO
     * @return 是否插入成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean insert(CreateAttributeValueDTO createDTO) {
        // 校验属性是否存在
        validateAttribute(createDTO.getAttributeId());

        MallAttributeValueEntity entity = new MallAttributeValueEntity();
        BeanUtils.copyProperties(createDTO, entity);

        // 设置默认值
        if (entity.getSort() == null) {
            entity.setSort(999);
        }

        entity.setIsDel(false);
        entity.setVersion(1);
        // 审计字段由 MyBatis 拦截器自动填充

        return attributeValueEntityMapper.insert(entity) > 0;
    }


    /**
     * 更新属性值
     * 采用乐观锁机制：version + CAS + retry
     * 审计字段由 MyBatis 拦截器自动填充
     *
     * @param updateDTO 更新属性值 DTO
     * @return 是否更新成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public boolean update(UpdateAttributeValueDTO updateDTO) {
        if (Objects.isNull(updateDTO) || Objects.isNull(updateDTO.getId())) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "属性值ID不能为空");
        }

        Long id = updateDTO.getId();
        MallAttributeValueEntity current = attributeValueEntityMapper.selectByPrimaryKey(id);
        if (Objects.isNull(current) || Boolean.TRUE.equals(current.getIsDel())) {
            throw new BusinessException("属性值不存在或已被删除");
        }

        // 如果修改了 attributeId，需要校验
        if (updateDTO.getAttributeId() != null && !updateDTO.getAttributeId().equals(current.getAttributeId())) {
            validateAttribute(updateDTO.getAttributeId());
        }

        // 保存当前版本号用于 CAS
        Integer oldVersion = current.getVersion();

        // 复制非空属性
        BeanCopyUtils.copyNonNullProperties(updateDTO, current);

        // 保持版本号为旧版本（用于 CAS 条件）
        current.setVersion(oldVersion);
        // 审计字段由 MyBatis 拦截器自动填充

        // CAS 更新（SQL 层保证 version 匹配后自增）
        int rows = attributeValueEntityMapper.updateWithVersion(current);
        if (rows == 0) {
            log.warn("Optimistic lock failure for attributeValueId {}", id);
            throw new OptimisticLockingFailureException("属性值数据已被修改，请刷新后重试");
        }

        return true;
    }


    /**
     * 批量删除属性值（逻辑删除）
     * 审计字段由 MyBatis 拦截器自动填充
     *
     * @param ids 属性值ID列表
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }

        MallAttributeValueEntity update = new MallAttributeValueEntity();
        update.setIsDel(true);
        // 审计字段由 MyBatis 拦截器自动填充

        MallAttributeValueEntityExample example = new MallAttributeValueEntityExample();
        example.createCriteria().andIdIn(ids).andIsDelEqualTo(false);

        return attributeValueEntityMapper.updateByExampleSelective(update, example) > 0;
    }

    private void validateAttribute(Long attributeId) {
        if (attributeId == null) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "属性ID不能为空");
        }
        MallAttributeEntity attribute = attributeEntityMapper.selectByPrimaryKey(attributeId);
        if (attribute == null || Boolean.TRUE.equals(attribute.getIsDel())) {
            throw new BusinessException("所属属性不存在或已被删除");
        }
    }
}
