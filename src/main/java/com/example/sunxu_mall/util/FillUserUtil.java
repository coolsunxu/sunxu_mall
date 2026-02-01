package com.example.sunxu_mall.util;

import com.example.sunxu_mall.constant.AuditFieldConstant;
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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 审计字段填充工具类
 * 用于自动填充实体类的创建人、更新人、时间等审计字段
 *
 * @author sunxu
 * @version 1.0
 * @date 2026/1/9 20:49
 */
@Slf4j
public class FillUserUtil {

    /**
     * 字段缓存，避免重复反射获取Field对象，提升性能
     * Key: Class对象, Value: (字段名 -> Field对象)
     */
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * 从缓存中获取 Field对象
     *
     * @param clazz     类对象
     * @param fieldName 字段名
     * @return Field对象，不存在返回null
     */
    private static Field getCachedField(Class<?> clazz, String fieldName) {
        return FIELD_CACHE
                .computeIfAbsent(clazz, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(fieldName, k -> getField(clazz, k));
    }

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
            setFieldIfNull(entity, AuditFieldConstant.CREATE_USER_ID, userId);
            setFieldIfNull(entity, AuditFieldConstant.CREATE_USER_NAME, userName);
            setFieldIfNull(entity, AuditFieldConstant.CREATE_TIME, now);
            setFieldIfNull(entity, AuditFieldConstant.IS_DEL, false);
            setFieldIfNull(entity, AuditFieldConstant.VERSION, 0);
        }

        setField(entity, AuditFieldConstant.UPDATE_USER_ID, userId);
        setField(entity, AuditFieldConstant.UPDATE_USER_NAME, userName);
        setField(entity, AuditFieldConstant.UPDATE_TIME, now);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = getCachedField(target.getClass(), fieldName);
            if (Objects.nonNull(field)) {
                field.setAccessible(true);
                field.set(target, value);
            }
        } catch (Exception e) {
            log.debug("Set audit field failed, field: {}, value: {}, reason: {}", fieldName, value, e.getMessage());
        }
    }

    private static void setFieldIfNull(Object target, String fieldName, Object value) {
        try {
            Field field = getCachedField(target.getClass(), fieldName);
            if (Objects.nonNull(field)) {
                field.setAccessible(true);
                Object currentValue = field.get(target);
                if (Objects.isNull(currentValue)) {
                    field.set(target, value);
                }
            }
        } catch (Exception e) {
            log.debug("Set audit field if null failed, field: {}, value: {}, reason: {}", fieldName, value, e.getMessage());
        }
    }

    /**
     * 递归获取字段（包含父类）
     *
     * @param clazz     类对象
     * @param fieldName 字段名
     * @return Field对象，不存在返回null
     */
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
