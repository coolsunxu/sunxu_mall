package com.example.sunxu_mall.helper;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.example.sunxu_mall.entity.auth.JwtUserEntity;
import com.example.sunxu_mall.errorcode.ErrorCode;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.util.RedisUtil;
import com.example.sunxu_mall.util.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

import static com.example.sunxu_mall.errorcode.ErrorCode.PERMISSION_DENIED;

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

    public TokenHelper(RedisUtil redisUtil) {
        super(redisUtil);
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
        // 先尝试从 Redis 中获取用户信息
        String userKey = getUserKey(username);
        String userDetailJson = redisUtil.get(userKey);

        if (StringUtils.hasLength(userDetailJson)) {
            try {
                // 如果 Redis 中有数据，解析为 JwtUserEntity
                return JSON.parseObject(userDetailJson, JwtUserEntity.class);
            } catch (Exception e) {
                log.warn("Failed to parse user details from Redis for user: {}, error: {}", username, e.getMessage());
                // 如果解析失败，回退到从数据库加载
                return loadUserDetailsByUsername(username);
            }
        }

        // 如果 Redis 中没有数据，从数据库加载
        return loadUserDetailsByUsername(username);
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

    /**
     * 从数据库加载用户详情信息
     *
     * @param username 用户名称
     * @return 用户详情
     */
    private UserDetails loadUserDetailsByUsername(String username) {
        try {
            // 注入 UserDetailsService 来加载用户信息
            UserDetailsService userDetailsService = SpringBeanUtil.getBean("userDetailsService");
            if (userDetailsService != null) {
                return userDetailsService.loadUserByUsername(username);
            }
            log.warn("UserDetailsService bean not found");
            return null;
        } catch (Exception e) {
            log.error("Failed to load user details for user: {}", username, e);
            throw new BusinessException(ErrorCode.OPERATION_FAILED.getCode(), "Failed to load user details: " + e.getMessage());
        }
    }

    /**
     * 获取当前登录的用户名称
     *
     * @return 用户名称
     */
    public String getCurrentUsername() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.isNull(authentication)) {
            log.warn("get a null authentication");
            throw new BusinessException(PERMISSION_DENIED.getCode(), "permission denied");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            // 如果是UserDetails对象，直接获取用户名
            return ((UserDetails) principal).getUsername();
        } else {
            log.warn("Unsupported principal type: {}, permission denied", principal.getClass().getName());
            throw new BusinessException(PERMISSION_DENIED.getCode(), "Unsupported principal type: " + principal.getClass().getName());
        }
    }
}