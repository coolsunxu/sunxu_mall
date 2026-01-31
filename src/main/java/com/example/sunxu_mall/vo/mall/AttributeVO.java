package com.example.sunxu_mall.vo.mall;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 属性展示对象
 */
@Data
@Schema(description = "属性展示对象")
public class AttributeVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "属性名称")
    private String name;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改人ID")
    private Long updateUserId;

    @Schema(description = "修改人名称")
    private String updateUserName;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

    @Schema(description = "版本号")
    private Integer version;
}
