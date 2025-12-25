package com.example.sunxu_mall.entity.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/24 19:41
 * @description
 */

@Schema(description = "权限用户实体")
@Data
public class AuthUserEntity {

    /**
     * 唯一标识
     */
    @NotBlank(message = "唯一标识不能为空")
    @Schema(description = "唯一标识")
    private String uuid;

    /**
     * 用户名称
     */
    @NotBlank(message = "用户名称不能为空")
    @Schema(description = "用户名称")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码")
    private String password;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    @Schema(description = "验证码")
    private String code;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;
}

