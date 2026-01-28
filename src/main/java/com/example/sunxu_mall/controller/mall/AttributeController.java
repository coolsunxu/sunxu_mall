package com.example.sunxu_mall.controller.mall;

import com.example.sunxu_mall.entity.mall.MallAttributeEntity;
import com.example.sunxu_mall.entity.mall.MallAttributeEntityExample;
import com.example.sunxu_mall.entity.mall.MallAttributeValueEntity;
import com.example.sunxu_mall.entity.mall.MallAttributeValueEntityExample;
import com.example.sunxu_mall.mapper.mall.MallAttributeEntityMapper;
import com.example.sunxu_mall.mapper.mall.MallAttributeValueEntityMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "属性管理", description = "商品属性及属性值接口")
@RestController
@RequestMapping("/v1/attribute")
public class AttributeController {

    private final MallAttributeEntityMapper attributeMapper;
    private final MallAttributeValueEntityMapper attributeValueMapper;

    public AttributeController(MallAttributeEntityMapper attributeMapper, MallAttributeValueEntityMapper attributeValueMapper) {
        this.attributeMapper = attributeMapper;
        this.attributeValueMapper = attributeValueMapper;
    }

    @Operation(summary = "获取所有属性及其值", description = "获取所有属性，并包含对应的属性值列表")
    @GetMapping("/allWithValues")
    public List<Map<String, Object>> allWithValues() {
        MallAttributeEntityExample attrExample = new MallAttributeEntityExample();
        attrExample.createCriteria().andIsDelEqualTo(false);
        List<MallAttributeEntity> attrs = attributeMapper.selectByExample(attrExample);

        MallAttributeValueEntityExample valueExample = new MallAttributeValueEntityExample();
        valueExample.createCriteria().andIsDelEqualTo(false);
        List<MallAttributeValueEntity> values = attributeValueMapper.selectByExample(valueExample);

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
}
