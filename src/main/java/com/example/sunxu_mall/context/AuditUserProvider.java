package com.example.sunxu_mall.context;

import com.example.sunxu_mall.entity.auth.JwtUserEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

/**
 * @author sunxu
 */

public class AuditUserProvider {
    public static final Long SYSTEM_USER_ID = 0L;
    public static final String SYSTEM_USER_NAME = "system";

    private static final AuditUser SYSTEM_USER = new AuditUser(SYSTEM_USER_ID, SYSTEM_USER_NAME);

    public static AuditUser getCurrentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.nonNull(authentication) && authentication.getPrincipal() instanceof JwtUserEntity) {
            JwtUserEntity jwtUserEntity = (JwtUserEntity) authentication.getPrincipal();
            return new AuditUser(jwtUserEntity.getId(), jwtUserEntity.getUsername());
        }

        return AuditContextHolder.get();
    }

    public static AuditUser getCurrentUserOrSystem() {
        AuditUser auditUser = getCurrentUserOrNull();
        return Objects.nonNull(auditUser) ? auditUser : SYSTEM_USER;
    }
}

