package com.example.sunxu_mall.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Excel 导出业务类型枚举
 */
@Getter
@AllArgsConstructor
public enum ExcelBizTypeEnum {

    MENU((byte) 1, "", "菜单", "", ""),
    ROLE((byte) 2, "", "角色", "", ""),
    DEPT((byte) 3, "", "部门", "", ""),
    USER((byte) 4, "com.example.sunxu_mall.dto.user.UserQueryDTO", "用户", "userService", "com.example.sunxu_mall.entity.sys.web.UserWebEntity"),
    JOB((byte) 5, "", "岗位", "", ""),
    
    UNIT((byte) 101, "", "单位", "", ""),
    BRAND((byte) 102, "", "品牌", "", ""),
    ATTRIBUTE((byte) 103, "", "属性", "", ""),
    ATTRIBUTE_VALUE((byte) 104, "", "属性值", "", ""),
    CATEGORY((byte) 105, "", "分类", "", ""),
    PRODUCT((byte) 106, "com.example.sunxu_mall.dto.mall.ProductQueryDTO", "商品", "productService", "com.example.sunxu_mall.entity.mall.ProductEntity"),
    
    COMMON_PHOTO_GROUP((byte) 110, "", "图片组", "", ""),
    COMMON_NOTIFY((byte) 111, "", "通知", "", ""),
    COMMON_JOB((byte) 112, "", "定时任务", "", ""),
    
    ORDER_TRADE((byte) 120, "", "订单", "", "");

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

    public static ExcelBizTypeEnum getByCode(Byte code) {
        for (ExcelBizTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
