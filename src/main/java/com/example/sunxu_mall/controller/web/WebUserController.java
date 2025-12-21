package com.example.sunxu_mall.controller.web;

import com.example.sunxu_mall.entity.sys.web.UserWebEntity;
import com.example.sunxu_mall.service.sys.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/web/user")
@Validated
public class WebUserController {

    private final UserService userService;

    public WebUserController(UserService userService) {
        this.userService = userService;
    }


    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @GetMapping(value = "/info")
    public UserWebEntity getUserInfo() {
        return userService.getUserInfo();
    }


}
