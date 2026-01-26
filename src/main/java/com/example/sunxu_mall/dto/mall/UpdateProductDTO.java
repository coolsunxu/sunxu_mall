package com.example.sunxu_mall.dto.mall;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品更新请求 DTO（字段白名单，仅包含允许更新的字段）
 *
 * @author sunxu
 */
@Data
@Schema(description = "商品更新请求")
public class UpdateProductDTO {

    @NotNull(message = "版本号不能为空")
    @Schema(description = "乐观锁版本号（必填，用于并发控制）", required = true)
    private Integer version;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "品牌ID")
    private Long brandId;

    @Schema(description = "单位ID")
    private Long unitId;

    @Schema(description = "产品组ID")
    private Long productGroupId;

    @Size(max = 200, message = "商品名称不能超过200个字符")
    @Schema(description = "商品名称")
    private String name;

    @Size(max = 100, message = "型号不能超过100个字符")
    @Schema(description = "型号")
    private String model;

    @Min(value = 0, message = "库存数量不能为负数")
    @Schema(description = "总库存数量")
    private Integer quantity;

    @Min(value = 0, message = "剩余库存不能为负数")
    @Schema(description = "剩余库存数量")
    private Integer remainQuantity;

    @DecimalMin(value = "0.00", message = "价格不能为负数")
    @Digits(integer = 10, fraction = 2, message = "价格格式不正确，最多2位小数")
    @Schema(description = "价格")
    private BigDecimal price;

    @Size(max = 500, message = "封面URL不能超过500个字符")
    @Schema(description = "封面图片URL")
    private String coverUrl;

    @Valid
    @Schema(description = "SKU属性列表（差量更新）")
    private List<UpdateProductAttributeDTO> skuAttributes;
}