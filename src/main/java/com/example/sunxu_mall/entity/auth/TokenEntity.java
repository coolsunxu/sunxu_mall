package com.example.sunxu_mall.entity.auth;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class TokenEntity {

    /**
     * 用户名称
     */
    private String username;

    /**
     * token
     */
    private String token;

    /**
     * 角色信息
     */
    private List<String> roles;

    /**
     * 过期时间
     */
    private int expiresIn;
}

