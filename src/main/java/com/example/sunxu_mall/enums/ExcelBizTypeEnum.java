package com.example.sunxu_mall.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Excel 导出业务类型枚举
 */
@Getter
@AllArgsConstructor
public enum ExcelBizTypeEnum {

    MENU((byte) 1, null, "菜单", null, null),
    ROLE((byte) 2, null, "角色", null, null),
    DEPT((byte) 3, null, "部门", null, null),
    USER((byte) 4, "com.example.sunxu_mall.dto.user.UserQueryDTO", "用户", "userService", "com.example.sunxu_mall.entity.sys.web.UserWebEntity"),
    JOB((byte) 5, null, "岗位", null, null),

    UNIT((byte) 101, null, "单位", null, null),
    BRAND((byte) 102, null, "品牌", null, null),
    ATTRIBUTE((byte) 103, null, "属性", null, null),
    ATTRIBUTE_VALUE((byte) 104, null, "属性值", null, null),
    CATEGORY((byte) 105, null, "分类", null, null),
    PRODUCT((byte) 106, "com.example.sunxu_mall.dto.mall.ProductQueryDTO", "商品", "productService", "com.example.sunxu_mall.entity.mall.ProductEntity"),

    COMMON_PHOTO_GROUP((byte) 110, null, "图片组", null, null),
    COMMON_NOTIFY((byte) 111, null, "通知", null, null),
    COMMON_JOB((byte) 112, null, "定时任务", null, null),

    ORDER_TRADE((byte) 120, null, "订单", null, null);

    /**
     * 任务 bizType
     */
    private final Byte code;

    /**
     * 请求实体类
     */
    private final String requestEntity;

    /**
     * 任务描述
     */
    private final String desc;

    /**
     * 服务名称
     */
    private final String serviceName;

    /**
     * 导出实体类
     */
    private final String exportClassName;

    /**
     * 根据code获取枚举
     *
     * @param code 业务类型code
     * @return ExcelBizTypeEnum，不存在返回null
     */
    public static ExcelBizTypeEnum getByCode(Byte code) {
        if (code == null) {
            return null;
        }
        for (ExcelBizTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 检查该业务类型是否已完整配置（可用于导出）
     *
     * @return true-已配置完整，false-未配置完整
     */
    public boolean isConfigured() {
        return StringUtils.hasText(requestEntity)
                && StringUtils.hasText(serviceName)
                && StringUtils.hasText(exportClassName);
    }

    /**
     * 获取请求实体类（Optional包装）
     *
     * @return Optional包装的请求实体类名
     */
    public Optional<String> getRequestEntityOpt() {
        return Optional.ofNullable(requestEntity);
    }

    /**
     * 获取服务名称（Optional包装）
     *
     * @return Optional包装的服务名称
     */
    public Optional<String> getServiceNameOpt() {
        return Optional.ofNullable(serviceName);
    }

    /**
     * 获取导出实体类（Optional包装）
     *
     * @return Optional包装的导出实体类名
     */
    public Optional<String> getExportClassNameOpt() {
        return Optional.ofNullable(exportClassName);
    }
}
