package com.example.sunxu_mall.dto.mall;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Update Attribute Value DTO")
public class UpdateAttributeValueDTO {
    @Schema(description = "ID", required = true)
    private Long id;

    @Schema(description = "Attribute ID", required = true)
    private Long attributeId;

    @Schema(description = "Attribute value", required = true)
    private String value;

    @Schema(description = "Sort", example = "999")
    private Integer sort;
}
