package com.example.sunxu_mall.service.mall;

import com.example.sunxu_mall.entity.mall.MallAttributeEntity;
import com.example.sunxu_mall.entity.mall.MallAttributeEntityExample;
import com.example.sunxu_mall.entity.mall.MallAttributeValueEntity;
import com.example.sunxu_mall.entity.mall.MallAttributeValueEntityExample;
import com.example.sunxu_mall.mapper.mall.MallAttributeEntityMapper;
import com.example.sunxu_mall.mapper.mall.MallAttributeValueEntityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttributeService {

    private final MallAttributeEntityMapper attributeEntityMapper;

    private final MallAttributeValueEntityMapper attributeValueEntityMapper;

    public AttributeService(
            MallAttributeEntityMapper attributeEntityMapper,
            MallAttributeValueEntityMapper attributeValueEntityMapper
    ) {
        this.attributeEntityMapper = attributeEntityMapper;
        this.attributeValueEntityMapper = attributeValueEntityMapper;
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


    @Transactional
    public boolean insert(MallAttributeEntity attribute) {
        // 设置默认值
        attribute.setIsDel(false);
        attribute.setVersion(1);
        attribute.setCreateTime(LocalDateTime.now());
        attribute.setUpdateTime(LocalDateTime.now());

        // 这里可以从上下文获取用户信息，暂时使用默认值
        attribute.setCreateUserId(1L);
        attribute.setCreateUserName("admin");
        attribute.setUpdateUserId(1L);
        attribute.setUpdateUserName("admin");

        return attributeEntityMapper.insert(attribute) > 0;
    }


    @Transactional
    public boolean update(MallAttributeEntity attribute) {
        MallAttributeEntity dbAttribute = attributeEntityMapper.selectByPrimaryKey(attribute.getId());
        if (dbAttribute == null) {
            return false;
        }

        // 更新字段
        dbAttribute.setName(attribute.getName());
        dbAttribute.setUpdateTime(LocalDateTime.now());
        dbAttribute.setUpdateUserId(1L);
        dbAttribute.setUpdateUserName("admin");
        dbAttribute.setVersion(dbAttribute.getVersion() + 1);

        return attributeEntityMapper.updateByPrimaryKey(dbAttribute) > 0;
    }


    @Transactional
    public boolean deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            MallAttributeEntity attribute = attributeEntityMapper.selectByPrimaryKey(id);
            if (attribute != null) {
                attribute.setIsDel(true);
                attribute.setUpdateTime(LocalDateTime.now());
                attribute.setUpdateUserId(1L);
                attribute.setUpdateUserName("admin");
                attribute.setVersion(attribute.getVersion() + 1);
                attributeEntityMapper.updateByPrimaryKey(attribute);
            }
        }
        return true;
    }


    public List<MallAttributeEntity> getAll() {
        MallAttributeEntityExample example = new MallAttributeEntityExample();
        example.createCriteria().andIsDelEqualTo(false);
        return attributeEntityMapper.selectByExample(example);
    }
}
