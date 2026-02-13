package com.example.sunxu_mall.controller.mall;

import com.example.sunxu_mall.annotation.Idempotency;
import com.example.sunxu_mall.dto.mall.AttributeQueryDTO;
import com.example.sunxu_mall.dto.mall.CreateAttributeDTO;
import com.example.sunxu_mall.dto.mall.UpdateAttributeDTO;
import com.example.sunxu_mall.model.ResponseCursorEntity;
import com.example.sunxu_mall.service.mall.AttributeService;
import com.example.sunxu_mall.vo.mall.AttributeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 属性管理控制器
 *
 * @author sunxu
 */
@Tag(name = "属性管理", description = "商品属性及属性值接口")
@RestController
@RequestMapping("/v1/attribute")
public class AttributeController {

    private final AttributeService attributeService;

    public AttributeController(AttributeService attributeService) {
        this.attributeService = attributeService;
    }

    /**
     * 查询属性列表（游标分页）
     *
     * @param queryDTO 查询条件
     * @return 游标分页结果
     */
    @Operation(summary = "查询属性列表（游标分页）", description = "支持向前/向后翻页，可指定页码")
    @PostMapping("/searchByBidirectionalCursor")
    public ResponseCursorEntity<AttributeVO> searchByBidirectionalCursor(@RequestBody AttributeQueryDTO queryDTO) {
        return attributeService.searchByBidirectionalCursor(queryDTO);
    }

    /**
     * 获取所有属性
     *
     * @return 属性列表
     */
    @Operation(summary = "获取所有属性", description = "获取所有属性列表")
    @GetMapping("/all")
    public ResponseEntity<List<AttributeVO>> getAll() {
        return ResponseEntity.ok(attributeService.getAll());
    }

    /**
     * 获取所有属性及其值
     *
     * @return 属性及其值列表
     */
    @Operation(summary = "获取所有属性及其值", description = "获取所有属性及其对应的属性值列表")
    @GetMapping("/allWithValues")
    public ResponseEntity<List<Map<String, Object>>> getAllWithValues() {
        return ResponseEntity.ok(attributeService.getAllWithValues());
    }

    /**
     * 新增属性
     *
     * @param createDTO 新增属性请求
     * @return 新增后的属性信息
     */
    @Operation(summary = "新增属性", description = "新增商品属性")
    @Idempotency
    @PostMapping("/insert")
    public ResponseEntity<AttributeVO> insert(@Valid @RequestBody CreateAttributeDTO createDTO) {
        AttributeVO created = attributeService.insert(createDTO);
        return ResponseEntity.ok(created);
    }

    /**
     * 更新属性
     *
     * @param updateDTO 更新属性请求
     * @return 更新后的属性信息
     */
    @Operation(summary = "更新属性", description = "更新商品属性")
    @Idempotency
    @PostMapping("/update")
    public ResponseEntity<AttributeVO> update(@Valid @RequestBody UpdateAttributeDTO updateDTO) {
        AttributeVO updated = attributeService.update(updateDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * 批量删除属性
     *
     * @param ids 属性ID列表
     * @return 删除结果
     */
    @Operation(summary = "批量删除属性", description = "根据 ID 列表批量删除商品属性")
    @Idempotency
    @PostMapping("/deleteByIds")
    public ResponseEntity<Boolean> deleteByIds(@RequestBody List<Long> ids) {
        boolean result = attributeService.deleteByIds(ids);
        return ResponseEntity.ok(result);
    }
}
