package com.example.sunxu_mall.dto.mall;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author sunxu
 */

@Data
@Schema(description = "Create Attribute Value DTO")
public class CreateAttributeValueDTO {
    @Schema(description = "Attribute ID", required = true)
    private Long attributeId;

    @Schema(description = "Attribute value", required = true)
    private String value;

    @Schema(description = "Sort", example = "999")
    private Integer sort;
}
