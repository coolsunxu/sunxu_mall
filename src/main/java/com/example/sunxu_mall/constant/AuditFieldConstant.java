package com.example.sunxu_mall.constant;

import java.util.Set;

/**
 * 审计字段常量类
 * 统一管理数据库审计字段名称
 *
 * @author sunxu
 * @version 1.0
 * @date 2026/2/1
 */
public final class AuditFieldConstant {

    private AuditFieldConstant() {
    }

    /**
     * 创建人ID
     */
    public static final String CREATE_USER_ID = "createUserId";

    /**
     * 创建人名称
     */
    public static final String CREATE_USER_NAME = "createUserName";

    /**
     * 创建时间
     */
    public static final String CREATE_TIME = "createTime";

    /**
     * 更新人ID
     */
    public static final String UPDATE_USER_ID = "updateUserId";

    /**
     * 更新人名称
     */
    public static final String UPDATE_USER_NAME = "updateUserName";

    /**
     * 更新时间
     */
    public static final String UPDATE_TIME = "updateTime";

    /**
     * 删除标记
     */
    public static final String IS_DEL = "isDel";

    /**
     * 乐观锁版本号
     */
    public static final String VERSION = "version";

    /**
     * 所有审计字段集合，可用于动态检查
     */
    public static final Set<String> ALL_AUDIT_FIELDS = Set.of(
            CREATE_USER_ID,
            CREATE_USER_NAME,
            CREATE_TIME,
            UPDATE_USER_ID,
            UPDATE_USER_NAME,
            UPDATE_TIME,
            IS_DEL,
            VERSION
    );

    /**
     * 插入时填充的字段集合
     */
    public static final Set<String> INSERT_FIELDS = Set.of(
            CREATE_USER_ID,
            CREATE_USER_NAME,
            CREATE_TIME,
            UPDATE_USER_ID,
            UPDATE_USER_NAME,
            UPDATE_TIME,
            IS_DEL,
            VERSION
    );

    /**
     * 更新时填充的字段集合
     */
    public static final Set<String> UPDATE_FIELDS = Set.of(
            UPDATE_USER_ID,
            UPDATE_USER_NAME,
            UPDATE_TIME
    );
}
