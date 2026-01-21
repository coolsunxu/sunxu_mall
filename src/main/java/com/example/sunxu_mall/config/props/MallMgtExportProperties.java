package com.example.sunxu_mall.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * mall.mgt 导出相关配置
 *
 * 对应配置项：
 * - mall.mgt.exportPageSize
 * - mall.mgt.sheetDataSize
 * - mall.mgt.temp-path
 */
@Data
@Component
@ConfigurationProperties(prefix = "mall.mgt")
public class MallMgtExportProperties {

    /**
     * 每次游标拉取条数（影响 DB 压力与导出速度）
     */
    private int exportPageSize = 100;

    /**
     * 单个 Sheet 最大行数（超过则切换新 Sheet）
     */
    private int sheetDataSize = 50_000;

    /**
     * 临时文件目录
     */
    private String tempPath = "./temp/";
}

