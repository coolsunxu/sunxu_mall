package com.example.sunxu_mall.controller.mall;

import com.example.sunxu_mall.dto.mall.AttributeValueQueryDTO;
import com.example.sunxu_mall.dto.mall.CreateAttributeValueDTO;
import com.example.sunxu_mall.dto.mall.UpdateAttributeValueDTO;
import com.example.sunxu_mall.model.ResponseCursorEntity;
import com.example.sunxu_mall.service.mall.AttributeValueService;
import com.example.sunxu_mall.vo.mall.AttributeValueVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author sunxu
 */

@Tag(name = "属性值管理", description = "商品属性值接口")
@RestController
@RequestMapping("/v1/attributeValue")
public class AttributeValueController {

    private final AttributeValueService attributeValueService;

    public AttributeValueController(AttributeValueService attributeValueService) {
        this.attributeValueService = attributeValueService;
    }

    @Operation(summary = "查询属性值列表（游标分页）", description = "支持向前/向后翻页，可指定页码")
    @PostMapping("/searchByBidirectionalCursor")
    public ResponseCursorEntity<AttributeValueVO> searchByBidirectionalCursor(@RequestBody AttributeValueQueryDTO queryDTO) {
        return attributeValueService.searchByBidirectionalCursor(queryDTO);
    }

    @Operation(summary = "新增属性值", description = "新增商品属性值")
    @PostMapping("/insert")
    public Map<String, Object> insert(@RequestBody CreateAttributeValueDTO createDTO) {
        boolean result = attributeValueService.insert(createDTO);
        return Map.of(
                "success", result
        );
    }

    @Operation(summary = "更新属性值", description = "更新商品属性值")
    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody UpdateAttributeValueDTO updateDTO) {
        boolean result = attributeValueService.update(updateDTO);
        return Map.of(
                "success", result
        );
    }

    @Operation(summary = "批量删除属性值", description = "根据 ID 列表批量删除商品属性值")
    @PostMapping("/deleteByIds")
    public Map<String, Object> deleteByIds(@RequestBody List<Long> ids) {
        boolean result = attributeValueService.deleteByIds(ids);
        return Map.of(
                "success", result
        );
    }
}
