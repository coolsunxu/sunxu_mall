package com.example.sunxu_mall.controller.mall;

import com.example.sunxu_mall.annotation.ExcelExport;
import com.example.sunxu_mall.annotation.Idempotency;
import com.example.sunxu_mall.convert.mall.ProductStructMapper;
import com.example.sunxu_mall.dto.mall.CreateProductDTO;
import com.example.sunxu_mall.dto.mall.ProductQueryDTO;
import com.example.sunxu_mall.dto.mall.UpdateProductDTO;
import com.example.sunxu_mall.entity.mall.ProductEntity;
import com.example.sunxu_mall.enums.ExcelBizTypeEnum;
import com.example.sunxu_mall.model.ResponseCursorEntity;
import com.example.sunxu_mall.service.mall.ProductService;
import com.example.sunxu_mall.vo.mall.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @author sunxu
 * @version 1.0
 * @date 2026/1/19 20:21
 * @description
 */

@Tag(name = "商品操作", description = "商品接口")
@RestController
@RequestMapping("/v1/product")
public class ProductController {

    private final ProductService productService;
    private final ProductStructMapper productStructMapper;


    public ProductController(ProductService productService, ProductStructMapper productStructMapper) {
        this.productService = productService;
        this.productStructMapper = productStructMapper;
    }

    /**
     * 新增商品
     *
     * @param request 新增商品请求
     * @return 新增后的商品信息
     */
    @Operation(summary = "新增商品", description = "创建新商品，涉及商品组、属性、详情等多个表的写入")
    @Idempotency
    @PostMapping
    public ResponseEntity<ProductVO> create(@Valid @RequestBody CreateProductDTO request) {
        ProductEntity created = productService.createProduct(request);
        return ResponseEntity.ok(productStructMapper.toVO(created));
    }

    /**
     * 通过 id查询商品信息
     *
     * @param id 系统 ID
     * @return 商品信息
     */
    @Operation(summary = "通过id查询商品信息", description = "通过id查询商品信息")
    @GetMapping("/findById")
    public ProductEntity findById(Long id) {
        return productService.findById(id);
    }

    /**
     * 查询商品列表（游标分页）
     * 注意：已移除 searchByPage 和 searchByCursor，统一使用此接口
     */
    @Operation(summary = "查询商品列表（游标分页）", description = "支持向前/向后翻页，可指定页码")
    @PostMapping("/searchByBidirectionalCursor")
    public ResponseCursorEntity<ProductVO> searchByBidirectionalCursor(@RequestBody ProductQueryDTO productQueryDTO) {
        ResponseCursorEntity<ProductEntity> cursorEntity = productService.searchByBidirectionalCursor(productQueryDTO);
        return ResponseCursorEntity.convert(cursorEntity, productStructMapper::toVOList);
    }

    /**
     * 更新商品信息
     * 采用乐观锁（CAS）控制并发：客户端必须提供 version，版本不匹配返回 409
     *
     * @param id      商品ID
     * @param request 更新请求（包含 version 用于并发控制）
     * @return 更新后的商品信息
     */
    @Operation(summary = "更新商品信息", description = "使用乐观锁控制并发，客户端需传入当前版本号，版本冲突返回409")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "商品不存在"),
            @ApiResponse(responseCode = "409", description = "版本冲突，数据已被其他用户修改，请刷新后重试")
    })
    @Idempotency
    @PutMapping("/{id}")
    public ResponseEntity<ProductVO> update(
            @Parameter(description = "商品ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductDTO request) {
        ProductEntity updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(productStructMapper.toVO(updated));
    }

    /**
     * 删除商品（软删除）
     */
    @Operation(summary = "删除商品", description = "软删除商品，并级联软删详情/属性/图片等关联数据")
    @Idempotency
    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> delete(
            @Parameter(description = "商品ID", required = true)
            @PathVariable Long id
    ) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(true);
    }

    /**
     * 导出商品数据, 通过注解异步实现
     */
    @ExcelExport(ExcelBizTypeEnum.PRODUCT)
    @Operation(summary = "导出商品数据", description = "导出商品数据")
    @Idempotency(ttlSeconds = 30)
    @PostMapping("/export")
    public void export(@RequestBody ProductQueryDTO productQueryDTO) {
        // Method body is empty as logic is handled by Aspect
    }

}
