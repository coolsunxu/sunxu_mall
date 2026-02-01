package com.example.sunxu_mall.constant;

/**
 * Excel导出任务常量类
 * 统一管理导出任务相关的常量
 *
 * @author sunxu
 * @version 1.0
 * @date 2026/2/1
 */
public final class ExportConstant {

    private ExportConstant() {
    }

    // ==================== 任务状态相关 ====================

    /**
     * 通知类型：系统通知
     */
    public static final byte NOTIFY_TYPE_SYSTEM = 1;

    /**
     * 读取状态：未读
     */
    public static final byte READ_STATUS_UNREAD = 0;

    /**
     * 读取状态：已读
     */
    public static final byte READ_STATUS_READ = 1;

    // ==================== 文件相关 ====================

    /**
     * 导出文件名格式：{业务名称}数据_{时间}
     */
    public static final String FILE_NAME_FORMAT = "%s数据_%s";

    /**
     * Excel文件Content-Type
     */
    public static final String EXCEL_CONTENT_TYPE = "application/vnd.ms-excel";

    /**
     * Excel文件扩展名
     */
    public static final String EXCEL_EXTENSION = ".xlsx";

    // ==================== 任务配置 ====================

    /**
     * 导出任务最大失败重试次数
     * 与 NumberConstant.MAX_EXPORT_FAILURE_COUNT 保持一致
     */
    public static final int MAX_FAILURE_COUNT = 3;

    /**
     * 默认导出分页大小
     * 与 NumberConstant.DEFAULT_EXPORT_PAGE_SIZE 保持一致
     */
    public static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * 默认每Sheet最大数据条数
     * 与 NumberConstant.DEFAULT_SHEET_DATA_SIZE 保持一致
     */
    public static final int DEFAULT_SHEET_SIZE = 50000;
}
