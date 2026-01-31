package com.example.sunxu_mall.dto.mall;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 更新属性请求 DTO
 */
@Data
@Schema(description = "更新属性请求")
public class UpdateAttributeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "属性ID不能为空")
    @Schema(description = "属性ID", required = true)
    private Long id;

    @NotBlank(message = "属性名称不能为空")
    @Size(max = 100, message = "属性名称不能超过100个字符")
    @Schema(description = "属性名称", required = true)
    private String name;
}
