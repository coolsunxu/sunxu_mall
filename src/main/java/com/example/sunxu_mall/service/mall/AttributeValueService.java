package com.example.sunxu_mall.service.mall;

import com.example.sunxu_mall.dto.mall.AttributeValueQueryDTO;
import com.example.sunxu_mall.dto.mall.CreateAttributeValueDTO;
import com.example.sunxu_mall.dto.mall.UpdateAttributeValueDTO;
import com.example.sunxu_mall.entity.auth.JwtUserEntity;
import com.example.sunxu_mall.entity.mall.MallAttributeEntity;
import com.example.sunxu_mall.entity.mall.MallAttributeValueEntity;
import com.example.sunxu_mall.entity.mall.MallAttributeValueEntityExample;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.mapper.mall.MallAttributeEntityMapper;
import com.example.sunxu_mall.mapper.mall.MallAttributeValueEntityMapper;
import com.example.sunxu_mall.model.ResponseCursorEntity;
import com.example.sunxu_mall.service.BaseService;
import com.example.sunxu_mall.util.SecurityUtil;
import com.example.sunxu_mall.vo.mall.AttributeValueVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.example.sunxu_mall.errorcode.ErrorCode.PARAMETER_MISSING;

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


    @Deprecated
    public Map<String, Object> searchByPage(AttributeValueQueryDTO queryDTO) {
        // 保留旧接口逻辑，但不推荐使用
        MallAttributeValueEntityExample example = new MallAttributeValueEntityExample();
        MallAttributeValueEntityExample.Criteria criteria = example.createCriteria();
        criteria.andIsDelEqualTo(false);

        if (queryDTO.getAttributeId() != null) {
            criteria.andAttributeIdEqualTo(queryDTO.getAttributeId());
        }
        if (queryDTO.getValue() != null && !queryDTO.getValue().isEmpty()) {
            criteria.andValueLike("%" + queryDTO.getValue() + "%");
        }

        example.setOrderByClause("sort asc, create_time desc");
        List<MallAttributeValueEntity> list = attributeValueEntityMapper.selectByExample(example);

        long total = list.size();
        Map<String, Object> result = new HashMap<>();
        result.put("rows", list);
        result.put("total", total);
        result.put("pageNum", queryDTO.getPageNum());
        result.put("pageSize", queryDTO.getPageSize());

        return result;
    }


    public ResponseCursorEntity<AttributeValueVO> searchByBidirectionalCursor(AttributeValueQueryDTO queryDTO) {
        return super.searchByBidirectionalCursor(queryDTO);
    }


    protected List<AttributeValueVO> selectListWithLimit(AttributeValueQueryDTO query, int limit) {
        return attributeValueEntityMapper.selectListWithLimit(
                query.getAttributeId(),
                query.getValue(),
                limit
        );
    }


    protected List<AttributeValueVO> selectListByCursorWithLimit(AttributeValueQueryDTO query, Long cursorId, int limit) {
        return attributeValueEntityMapper.selectByCursorWithLimit(
                query.getAttributeId(),
                query.getValue(),
                cursorId,
                limit
        );
    }


    protected Long extractEntityId(AttributeValueVO entity) {
        return Objects.isNull(entity) ? null : entity.getId();
    }


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

        JwtUserEntity currentUser = SecurityUtil.getUserInfo();
        LocalDateTime now = LocalDateTime.now();

        entity.setIsDel(false);
        entity.setVersion(1);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setCreateUserId(currentUser.getId());
        entity.setCreateUserName(currentUser.getUsername());
        entity.setUpdateUserId(currentUser.getId());
        entity.setUpdateUserName(currentUser.getUsername());

        return attributeValueEntityMapper.insert(entity) > 0;
    }


    @Transactional(rollbackFor = Exception.class)
    public boolean update(UpdateAttributeValueDTO updateDTO) {
        if (updateDTO.getId() == null) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "属性值ID不能为空");
        }

        MallAttributeValueEntity entity = attributeValueEntityMapper.selectByPrimaryKey(updateDTO.getId());
        if (entity == null || Boolean.TRUE.equals(entity.getIsDel())) {
            throw new BusinessException("属性值不存在或已被删除");
        }

        // 如果修改了 attributeId，需要校验
        if (updateDTO.getAttributeId() != null && !updateDTO.getAttributeId().equals(entity.getAttributeId())) {
            validateAttribute(updateDTO.getAttributeId());
        }

        BeanUtils.copyProperties(updateDTO, entity);

        JwtUserEntity currentUser = SecurityUtil.getUserInfo();
        entity.setUpdateTime(LocalDateTime.now());
        entity.setUpdateUserId(currentUser.getId());
        entity.setUpdateUserName(currentUser.getUsername());
        // 乐观锁版本增加
        entity.setVersion(entity.getVersion() + 1);

        return attributeValueEntityMapper.updateByPrimaryKeySelective(entity) > 0;
    }


    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }

        JwtUserEntity currentUser = SecurityUtil.getUserInfo();
        LocalDateTime now = LocalDateTime.now();

        MallAttributeValueEntity update = new MallAttributeValueEntity();
        update.setIsDel(true);
        update.setUpdateTime(now);
        update.setUpdateUserId(currentUser.getId());
        update.setUpdateUserName(currentUser.getUsername());

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
