package com.example.sunxu_mall.util;

import com.example.sunxu_mall.context.AuditUser;
import com.example.sunxu_mall.context.AuditUserProvider;
import com.example.sunxu_mall.entity.common.CommonTaskEntity;
import com.example.sunxu_mall.errorcode.ErrorCode;
import com.example.sunxu_mall.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author sunxu
 * @version 1.0
 * @date 2026/1/9 20:49
 * @description
 */
@Slf4j
public class FillUserUtil {

    private static final String CREATE_USER_ID = "createUserId";
    private static final String CREATE_USER_NAME = "createUserName";
    private static final String CREATE_TIME = "createTime";
    private static final String UPDATE_USER_ID = "updateUserId";
    private static final String UPDATE_USER_NAME = "updateUserName";
    private static final String UPDATE_TIME = "updateTime";
    private static final String IS_DEL = "isDel";
    private static final String VERSION = "version";

    /**
     * 插入时填充审计字段
     *
     * @param entity 实体对象
     */
    public static void fillInsert(Object entity) {
        fillUserAndDate(entity, true);
    }

    /**
     * 更新时填充审计字段
     *
     * @param entity 实体对象
     */
    public static void fillUpdate(Object entity) {
        fillUserAndDate(entity, false);
    }

    private static void fillUserAndDate(Object entity, boolean isInsert) {
        AuditUser auditUser = AuditUserProvider.getCurrentUserOrSystem();
        Long userId = auditUser.getUserId();
        String userName = auditUser.getUserName();

        LocalDateTime now = LocalDateTime.now();

        if (isInsert) {
            setFieldIfNull(entity, CREATE_USER_ID, userId);
            setFieldIfNull(entity, CREATE_USER_NAME, userName);
            setFieldIfNull(entity, CREATE_TIME, now);
            setFieldIfNull(entity, IS_DEL, false);
            setFieldIfNull(entity, VERSION, 0);
        }

        setField(entity, UPDATE_USER_ID, userId);
        setField(entity, UPDATE_USER_NAME, userName);
        setField(entity, UPDATE_TIME, now);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = getField(target.getClass(), fieldName);
            if (Objects.nonNull(field)) {
                // 设置属性可以访问
                field.setAccessible(true);
                field.set(target, value);
            }
        } catch (Exception e) {
            // 忽略字段不存在的情况
        }
    }
    
    private static void setFieldIfNull(Object target, String fieldName, Object value) {
        try {
            Field field = getField(target.getClass(), fieldName);
            if (Objects.nonNull(field)) {
                // 设置属性可以访问
                field.setAccessible(true);
                Object currentValue = field.get(target);
                if (Objects.isNull(currentValue)) {
                    field.set(target, value);
                }
            }
        } catch (Exception e) {
            // 忽略
        }
    }

    private static Field getField(Class<?> clazz, String fieldName) {
        if (Objects.isNull(clazz) || clazz == Object.class) {
            return null;
        }
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return getField(clazz.getSuperclass(), fieldName);
        }
    }

    /**
     * 兼容旧方法，建议使用 fillInsert
     */
    public static void fillCreateUserInfo(CommonTaskEntity commonTaskEntity) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.isNull(authentication)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "Unauthorized");
        }
        fillInsert(commonTaskEntity);
    }

    /**
     * 保持不变，供 ExcelExportTask 使用
     */
    public static void fillUpdateUserInfoFromCreate(CommonTaskEntity baseEntity) {
        baseEntity.setUpdateUserId(baseEntity.getCreateUserId());
        baseEntity.setUpdateUserName(baseEntity.getCreateUserName());
    }
}
