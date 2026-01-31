package com.example.sunxu_mall.dto.mall;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 新增属性请求 DTO
 * @author sunxu
 */

@Data
@Schema(description = "新增属性请求")
public class CreateAttributeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "属性名称不能为空")
    @Size(max = 100, message = "属性名称不能超过100个字符")
    @Schema(description = "属性名称", required = true)
    private String name;
}
