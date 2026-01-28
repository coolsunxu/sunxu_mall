package com.example.sunxu_mall.dto.mall;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 新增商品请求 DTO
 */
@Data
@Schema(description = "新增商品请求")
public class CreateProductDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID", required = true)
    private Long categoryId;

    @NotNull(message = "品牌ID不能为空")
    @Schema(description = "品牌ID", required = true)
    private Long brandId;

    @NotNull(message = "单位ID不能为空")
    @Schema(description = "单位ID", required = true)
    private Long unitId;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称不能超过200个字符")
    @Schema(description = "商品名称", required = true)
    private String name;

    @Size(max = 100, message = "型号不能超过100个字符")
    @Schema(description = "型号")
    private String model;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能为负数")
    @Digits(integer = 10, fraction = 2, message = "价格格式不正确，最多2位小数")
    @Schema(description = "价格", required = true)
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存数量不能为负数")
    @Schema(description = "总库存数量", required = true)
    private Integer quantity;

    @Size(max = 500, message = "封面URL不能超过500个字符")
    @Schema(description = "封面图片URL")
    private String coverUrl;

    @Schema(description = "商品详情（HTML/富文本）")
    private String detail;

    @Valid
    @Schema(description = "SPU 属性列表（用于确定商品组）")
    private List<CreateProductAttributeDTO> spuAttributes;

    @Valid
    @Schema(description = "SKU 属性列表")
    private List<CreateProductAttributeDTO> skuAttributes;

    @Schema(description = "轮播图 URL 列表")
    private List<String> photos;
}
