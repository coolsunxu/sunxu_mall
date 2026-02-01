package com.example.sunxu_mall.constant;

/**
 * 数字常量类
 * 提供常用数字常量和业务语义化常量
 *
 * @author sunxu
 * @version 1.0
 * @date 2025/12/25 20:55
 */
public final class NumberConstant {

    private NumberConstant() {
    }

    // ==================== 基础数字常量 ====================
    public static final int NUMBER_1 = 1;
    public static final int NUMBER_2 = 2;
    public static final int NUMBER_3 = 3;
    public static final int NUMBER_4 = 4;
    public static final int NUMBER_5 = 5;
    public static final int NUMBER_6 = 6;
    public static final int NUMBER_7 = 7;
    public static final int NUMBER_8 = 8;
    public static final int NUMBER_9 = 9;
    public static final int NUMBER_10 = 10;
    public static final int NUMBER_20 = 20;
    public static final int NUMBER_30 = 30;
    public static final int NUMBER_40 = 40;
    public static final int NUMBER_50 = 50;
    public static final int NUMBER_60 = 60;
    public static final int NUMBER_70 = 70;
    public static final int NUMBER_80 = 80;
    public static final int NUMBER_90 = 90;
    public static final int NUMBER_100 = 100;
    public static final int NUMBER_200 = 200;
    public static final int NUMBER_500 = 500;
    public static final int NUMBER_1000 = 1000;
    public static final int NUMBER_10000 = 10000;

    // ==================== 业务语义化常量 ====================

    /**
     * 导出任务最大失败重试次数
     */
    public static final int MAX_EXPORT_FAILURE_COUNT = 3;

    /**
     * 默认导出分页大小
     */
    public static final int DEFAULT_EXPORT_PAGE_SIZE = 100;

    /**
     * 默认每Sheet最大数据条数
     */
    public static final int DEFAULT_SHEET_DATA_SIZE = 50000;
}
