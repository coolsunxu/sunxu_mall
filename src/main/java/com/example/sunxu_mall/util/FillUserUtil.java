package com.example.sunxu_mall.util;

import com.example.sunxu_mall.entity.auth.JwtUserEntity;
import com.example.sunxu_mall.entity.common.CommonTaskEntity;
import com.example.sunxu_mall.errorcode.ErrorCode;
import com.example.sunxu_mall.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.Objects;

/**
 * @author sunxu
 * @version 1.0
 * @date 2026/1/9 20:49
 * @description
 */
public class FillUserUtil {
    public static void fillCreateUserInfo(CommonTaskEntity commonTaskEntity) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.isNull(authentication)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "Unauthorized");
        }

        JwtUserEntity jwtUserEntity = (JwtUserEntity) authentication.getPrincipal();
        commonTaskEntity.setCreateUserId(jwtUserEntity.getId());
        commonTaskEntity.setCreateUserName(jwtUserEntity.getUsername());
        commonTaskEntity.setCreateTime(java.time.LocalDateTime.now());

    }

    public static void fillUpdateUserInfoFromCreate(CommonTaskEntity baseEntity) {
        baseEntity.setUpdateUserId(baseEntity.getCreateUserId());
        baseEntity.setUpdateUserName(baseEntity.getCreateUserName());
    }
}
