package com.example.sunxu_mall.controller.web;

import com.example.sunxu_mall.annotation.NoLogin;
import com.example.sunxu_mall.entity.auth.AuthUserEntity;
import com.example.sunxu_mall.entity.auth.CaptchaEntity;
import com.example.sunxu_mall.entity.auth.JwtUserEntity;
import com.example.sunxu_mall.entity.auth.TokenEntity;
import com.example.sunxu_mall.entity.sys.web.UserWebEntity;
import com.example.sunxu_mall.service.sys.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/web/user")
@Validated
@Tag(name = "Web用户管理", description = "Web端用户相关接口")
public class WebUserController {

    private final UserService userService;

    public WebUserController(UserService userService) {
        this.userService = userService;
    }


    /**
     * Get user information
     *
     * @return User information
     */
    @GetMapping(value = "/info")
    public JwtUserEntity getUserInfo() {
        return userService.getUserInfo();
    }

    /**
     * User phone login
     *
     * @param authUserEntity User entity
     * @return Affected rows
     */
    @NoLogin
    @Operation(summary = "用户登录", description = "用户通过手机号登录")
    @PostMapping("/login")
    public TokenEntity login(@Valid @RequestBody AuthUserEntity authUserEntity) {
        return userService.login(authUserEntity);
    }

    @NoLogin
    @Operation(summary = "获取验证码", description = "获取登录验证码")
    @GetMapping(value = "/code")
    public CaptchaEntity getCode() {
        return userService.getCode();
    }


}
