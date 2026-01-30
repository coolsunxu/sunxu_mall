package com.example.sunxu_mall.controller.mall;

import com.example.sunxu_mall.entity.mall.MallAttributeEntity;
import com.example.sunxu_mall.service.mall.AttributeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "属性管理", description = "商品属性及属性值接口")
@RestController
@RequestMapping("/v1/attribute")
public class AttributeController {

    @Autowired
    private AttributeService attributeService;

    @Operation(summary = "获取所有属性及其值", description = "获取所有属性，并包含对应的属性值列表")
    @GetMapping("/allWithValues")
    public List<Map<String, Object>> allWithValues() {
        return attributeService.getAllWithValues();
    }

    @Operation(summary = "获取所有属性", description = "获取所有属性列表")
    @GetMapping("/all")
    public List<MallAttributeEntity> getAll() {
        return attributeService.getAll();
    }

    @Operation(summary = "新增属性", description = "新增商品属性")
    @PostMapping("/insert")
    public Map<String, Object> insert(@RequestBody MallAttributeEntity attribute) {
        boolean result = attributeService.insert(attribute);
        return Map.of(
                "success", result
        );
    }

    @Operation(summary = "更新属性", description = "更新商品属性")
    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody MallAttributeEntity attribute) {
        boolean result = attributeService.update(attribute);
        return Map.of(
                "success", result
        );
    }

    @Operation(summary = "批量删除属性", description = "根据 ID 列表批量删除商品属性")
    @PostMapping("/deleteByIds")
    public Map<String, Object> deleteByIds(@RequestBody List<Long> ids) {
        boolean result = attributeService.deleteByIds(ids);
        return Map.of(
                "success", result
        );
    }
}
