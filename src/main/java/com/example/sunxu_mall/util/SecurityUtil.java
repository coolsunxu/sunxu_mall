package com.example.sunxu_mall.util;

import com.example.sunxu_mall.entity.auth.JwtUserEntity;
import com.example.sunxu_mall.errorcode.ErrorCode;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.helper.TokenHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Objects;

/**
 * Security Utility class
 * 统一获取当前用户信息的入口
 * 
 * 获取策略（按优先级）：
 * 1. 优先从 SecurityContext 获取（已认证的请求中直接获取，无需查库）
 * 2. 回退到 TokenHelper 获取（从 Redis 缓存获取）
 * 3. 最后回源数据库（TokenHelper 内部实现）
 *
 * @author sunxu
 * @version 1.0
 */
@Slf4j
public class SecurityUtil {

    /**
     * 获取当前登录用户信息
     * 优先从 SecurityContext 获取，避免重复查库
     *
     * @return 当前用户信息
     * @throws BusinessException 如果用户未登录或获取失败
     */
    public static JwtUserEntity getUserInfo() {
        // 优先从 SecurityContext 获取（已认证的请求中直接获取，无需查库）
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.nonNull(authentication) && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof JwtUserEntity) {
                return (JwtUserEntity) principal;
            }
            // 如果 principal 是 UserDetails 但不是 JwtUserEntity，尝试转换
            if (principal instanceof UserDetails) {
                log.debug("Principal is UserDetails but not JwtUserEntity, falling back to TokenHelper");
            }
        }

        // 回退到 TokenHelper 获取（从 Redis 缓存或数据库获取）
        TokenHelper tokenHelper = SpringBeanUtil.getBean(TokenHelper.class);
        if (Objects.isNull(tokenHelper)) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), "TokenHelper not available");
        }

        String currentUsername = tokenHelper.getCurrentUsername();
        UserDetails userDetails = tokenHelper.getUserDetailsFromUsername(currentUsername);
        if (userDetails instanceof JwtUserEntity) {
            return (JwtUserEntity) userDetails;
        }

        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), "Failed to load user info");
    }

    /**
     * 获取当前登录用户名
     *
     * @return 当前用户名
     * @throws BusinessException 如果用户未登录
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.nonNull(authentication) && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            }
            if (principal instanceof String) {
                return (String) principal;
            }
        }

        TokenHelper tokenHelper = SpringBeanUtil.getBean(TokenHelper.class);
        if (Objects.nonNull(tokenHelper)) {
            return tokenHelper.getCurrentUsername();
        }

        throw new BusinessException(ErrorCode.PERMISSION_DENIED.getCode(), "User not authenticated");
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 当前用户 ID
     * @throws BusinessException 如果用户未登录
     */
    public static Long getCurrentUserId() {
        return getUserInfo().getId();
    }

    /**
     * 检查当前用户是否已认证
     *
     * @return true 如果已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Objects.nonNull(authentication) && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
