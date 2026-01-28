package com.example.sunxu_mall.dto.mall;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 新增商品时的属性 DTO
 */
@Data
@Schema(description = "新增商品时的属性请求")
public class CreateProductAttributeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "属性ID不能为空")
    @Schema(description = "属性ID", required = true)
    private Long attributeId;

    @NotNull(message = "属性值ID不能为空")
    @Schema(description = "属性值ID", required = true)
    private Long attributeValueId;
}
