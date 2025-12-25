package com.example.sunxu_mall.helper;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.example.sunxu_mall.entity.auth.JwtUserEntity;
import com.example.sunxu_mall.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/24 20:13
 * @description
 */


@Slf4j
@Component
public class TokenHelper extends UserTokenHelper {

    private static final String TOKEN_PREFIX = "token:";
    private static final String USER_PREFIX = "user:";


    private final RedisUtil redisUtil;

    public TokenHelper(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }


    /**
     * 生成 token
     *
     * @param userDetails 用户信息
     * @return token
     */
    public String generateToken(UserDetails userDetails) {
        return super.generateToken(userDetails.getUsername(), JSON.toJSONString(userDetails));
    }


    /**
     * 根据用户名称查询用户详情信息
     *
     * @param username 用户名称
     * @return 用户详情
     */
    public UserDetails getUserDetailsFromUsername(String username) {
        String userKey = getUserKey(username);
        String userDetailJson = redisUtil.get(userKey);
        if (!StringUtils.hasLength(userDetailJson)) {
            return null;
        }

        return JSON.parseObject(userDetailJson, JwtUserEntity.class);
    }


    /**
     * 获取 token
     *
     * @param username 用户名称
     * @return token
     */
    public String getToken(String username) {
        return redisUtil.get(getKey(TOKEN_PREFIX, username));
    }

    /**
     * 删除 token
     *
     * @param token 用户名称
     */
    public void delToken(String token) {
        String username = getUsernameFromToken(token);
        redisUtil.delete(getKey(TOKEN_PREFIX, username));
        redisUtil.delete(getKey(USER_PREFIX, username));
    }

    /**
     * 获取用户详情
     *
     * @param username 用户名称
     * @return 用户详情
     */
    public UserDetails getUserDetails(String username) {
        String userJson = redisUtil.get(getKey(USER_PREFIX, username));
        return JSONUtil.toBean(userJson, UserDetails.class);
    }
}