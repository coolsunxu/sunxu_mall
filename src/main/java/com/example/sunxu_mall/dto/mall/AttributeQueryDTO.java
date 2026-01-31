package com.example.sunxu_mall.dto.mall;

import com.example.sunxu_mall.dto.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 属性查询 DTO
 *
 * @author sunxu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "属性查询条件")
public class AttributeQueryDTO extends BasePageQuery {

    @Schema(description = "属性名称")
    private String name;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}
