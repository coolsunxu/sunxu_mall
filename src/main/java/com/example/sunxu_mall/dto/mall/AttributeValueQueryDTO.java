package com.example.sunxu_mall.dto.mall;

import com.example.sunxu_mall.dto.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Attribute Value Query DTO")
public class AttributeValueQueryDTO extends BasePageQuery {
    @Schema(description = "Attribute ID")
    private Long attributeId;

    @Schema(description = "Attribute value")
    private String value;

    @Schema(description = "Sort")
    private Integer sort;

    @Schema(description = "Create user ID")
    private Long createUserId;

    @Schema(description = "Create user name")
    private String createUserName;

    @Schema(description = "Start time")
    private String startTime;

    @Schema(description = "End time")
    private String endTime;
}
