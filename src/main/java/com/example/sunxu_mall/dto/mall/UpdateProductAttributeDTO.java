package com.example.sunxu_mall.dto.mall;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品属性更新请求 DTO（用于差量更新）
 *
 * @author sunxu
 */
@Data
@Schema(description = "商品属性更新请求")
public class UpdateProductAttributeDTO {

    @Schema(description = "属性记录ID（更新/删除时必填，新增时为空）")
    private Long id;

    @NotNull(message = "属性ID不能为空")
    @Schema(description = "属性ID", required = true)
    private Long attributeId;

    @NotNull(message = "属性值ID不能为空")
    @Schema(description = "属性值ID", required = true)
    private Long attributeValueId;

    @Schema(description = "是否删除（true=软删除该属性）")
    private Boolean deleted;
}