package com.example.sunxu_mall.constant;

/**
 * @author sunxu
 * @description MQ常量定义
 */
public class MQConstant {

    /**
     * 通用任务 Topic
     */
    public static final String MALL_COMMON_TASK_TOPIC = "mall-task-execute-topic";

    /**
     * 通用任务创建 Topic (用于削峰)
     */
    public static final String MALL_COMMON_TASK_CREATE_TOPIC = "mall-task-create-topic";

    /**
     * 通知推送 Topic
     */
    public static final String MALL_NOTIFICATION_TOPIC = "mall-notify-topic";

    /**
     * Excel导出任务 Tag
     */
    public static final String TAG_EXCEL_EXPORT = "Tag_ExcelExport";

    /**
     * Excel导出任务创建请求 Tag
     */
    public static final String TAG_EXCEL_EXPORT_CREATE = "excel_export_create";

    /**
     * 任务完成通知 Tag
     */
    public static final String TAG_NOTIFICATION = "TAG_NOTIFICATION";
}
