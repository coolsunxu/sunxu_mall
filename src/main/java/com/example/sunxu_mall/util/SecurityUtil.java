package com.example.sunxu_mall.util;

import com.example.sunxu_mall.entity.auth.JwtUserEntity;
import com.example.sunxu_mall.errorcode.ErrorCode;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.helper.TokenHelper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * @author sunxu
 * @version 1.0
 * @description Security Utility class
 */
public class SecurityUtil {

    public static JwtUserEntity getUserInfo() {
        TokenHelper tokenHelper = SpringBeanUtil.getBean(TokenHelper.class);
        UserDetailsService userDetailsService = SpringBeanUtil.getBean(UserDetailsService.class);
        
        String currentUsername = tokenHelper.getCurrentUsername();
        UserDetails userDetails = userDetailsService.loadUserByUsername(currentUsername);
        if (userDetails instanceof JwtUserEntity) {
            return (JwtUserEntity) userDetails;
        }
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), "Failed to load user info");
    }
}
