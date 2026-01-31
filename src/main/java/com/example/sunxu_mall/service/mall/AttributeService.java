package com.example.sunxu_mall.service.mall;

import com.example.sunxu_mall.dto.mall.AttributeQueryDTO;
import com.example.sunxu_mall.dto.mall.CreateAttributeDTO;
import com.example.sunxu_mall.dto.mall.UpdateAttributeDTO;
import com.example.sunxu_mall.entity.mall.MallAttributeEntity;
import com.example.sunxu_mall.entity.mall.MallAttributeEntityExample;
import com.example.sunxu_mall.entity.mall.MallAttributeValueEntity;
import com.example.sunxu_mall.entity.mall.MallAttributeValueEntityExample;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.mapper.mall.MallAttributeEntityMapper;
import com.example.sunxu_mall.mapper.mall.MallAttributeValueEntityMapper;
import com.example.sunxu_mall.model.ResponseCursorEntity;
import com.example.sunxu_mall.service.BaseService;
import com.example.sunxu_mall.vo.mall.AttributeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.example.sunxu_mall.errorcode.ErrorCode.PARAMETER_MISSING;

/**
 * 属性服务类
 * 采用 MyBatis 拦截器自动填充审计字段，乐观锁实现并发控制
 *
 * @author sunxu
 * @version 1.0
 */
@Slf4j
@Service
public class AttributeService extends BaseService<AttributeVO, AttributeQueryDTO> {

    private final MallAttributeEntityMapper attributeEntityMapper;

    private final MallAttributeValueEntityMapper attributeValueEntityMapper;

    public AttributeService(
            MallAttributeEntityMapper attributeEntityMapper,
            MallAttributeValueEntityMapper attributeValueEntityMapper
    ) {
        this.attributeEntityMapper = attributeEntityMapper;
        this.attributeValueEntityMapper = attributeValueEntityMapper;
    }


    @Override
    public ResponseCursorEntity<AttributeVO> searchByBidirectionalCursor(AttributeQueryDTO queryDTO) {
        return super.searchByBidirectionalCursor(queryDTO);
    }

    @Override
    protected List<AttributeVO> selectListWithLimit(AttributeQueryDTO query, int limit) {
        return attributeEntityMapper.selectListWithLimit(
                query.getName(),
                limit
        );
    }

    @Override
    protected List<AttributeVO> selectListByCursorWithLimit(AttributeQueryDTO query, Long cursorId, int limit) {
        return attributeEntityMapper.selectByCursorWithLimit(
                query.getName(),
                cursorId,
                limit
        );
    }

    @Override
    protected Long extractEntityId(AttributeVO entity) {
        return Objects.isNull(entity) ? null : entity.getId();
    }


    public List<Map<String, Object>> getAllWithValues() {
        MallAttributeEntityExample attrExample = new MallAttributeEntityExample();
        attrExample.createCriteria().andIsDelEqualTo(false);
        List<MallAttributeEntity> attrs = attributeEntityMapper.selectByExample(attrExample);

        MallAttributeValueEntityExample valueExample = new MallAttributeValueEntityExample();
        valueExample.createCriteria().andIsDelEqualTo(false);
        List<MallAttributeValueEntity> values = attributeValueEntityMapper.selectByExample(valueExample);

        Map<Long, List<MallAttributeValueEntity>> valueMap = values.stream()
                .collect(Collectors.groupingBy(MallAttributeValueEntity::getAttributeId));

        return attrs.stream().map(attr -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", attr.getId());
            map.put("name", attr.getName());
            map.put("values", valueMap.getOrDefault(attr.getId(), new ArrayList<>()));
            return map;
        }).collect(Collectors.toList());
    }


    /**
     * 插入属性
     * 审计字段由 MyBatis 拦截器自动填充
     *
     * @param createDTO 创建属性 DTO
     * @return 属性 VO
     */
    @Transactional(rollbackFor = Exception.class)
    public AttributeVO insert(CreateAttributeDTO createDTO) {
        if (Objects.isNull(createDTO)) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "属性信息不能为空");
        }

        MallAttributeEntity attribute = new MallAttributeEntity();
        attribute.setName(createDTO.getName());

        // 审计字段由 MyBatis 拦截器自动填充

        attributeEntityMapper.insertSelective(attribute);

        return convertToVO(attribute);
    }


    /**
     * 更新属性
     * 采用乐观锁机制：version + CAS + retry
     * 审计字段由 MyBatis 拦截器自动填充
     *
     * @param updateDTO 更新属性 DTO
     * @return 属性 VO
     */
    @Transactional(rollbackFor = Exception.class)
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public AttributeVO update(UpdateAttributeDTO updateDTO) {
        if (Objects.isNull(updateDTO) || Objects.isNull(updateDTO.getId())) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "属性ID不能为空");
        }

        Long id = updateDTO.getId();
        MallAttributeEntity current = attributeEntityMapper.selectByPrimaryKey(id);
        if (Objects.isNull(current) || Boolean.TRUE.equals(current.getIsDel())) {
            throw new BusinessException("属性不存在或已被删除");
        }

        // 保存当前版本号用于 CAS
        Integer oldVersion = current.getVersion();

        // 更新字段
        current.setName(updateDTO.getName());

        // 保持版本号为旧版本（用于 CAS 条件）
        current.setVersion(oldVersion);
        // 审计字段由 MyBatis 拦截器自动填充

        // CAS 更新（SQL 层保证 version 匹配后自增）
        int rows = attributeEntityMapper.updateWithVersion(current);
        if (rows == 0) {
            log.warn("Optimistic lock failure for attributeId {}", id);
            throw new OptimisticLockingFailureException("属性数据已被修改，请刷新后重试");
        }

        // 重新查询并返回更新后的数据
        MallAttributeEntity updated = attributeEntityMapper.selectByPrimaryKey(id);
        return convertToVO(updated);
    }


    /**
     * 批量删除属性（逻辑删除）
     * 审计字段由 MyBatis 拦截器自动填充
     *
     * @param ids 属性ID列表
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }

        MallAttributeEntityExample example = new MallAttributeEntityExample();
        example.createCriteria().andIdIn(ids).andIsDelEqualTo(false);

        MallAttributeEntity update = new MallAttributeEntity();
        update.setIsDel(true);
        // 审计字段由 MyBatis 拦截器自动填充

        return attributeEntityMapper.updateByExampleSelective(update, example) > 0;
    }


    public List<AttributeVO> getAll() {
        MallAttributeEntityExample example = new MallAttributeEntityExample();
        example.createCriteria().andIsDelEqualTo(false);
        List<MallAttributeEntity> entities = attributeEntityMapper.selectByExample(example);

        return entities.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }


    /**
     * 将实体转换为VO
     *
     * @param entity 属性实体
     * @return 属性VO
     */
    private AttributeVO convertToVO(MallAttributeEntity entity) {
        if (Objects.isNull(entity)) {
            return null;
        }

        AttributeVO vo = new AttributeVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
