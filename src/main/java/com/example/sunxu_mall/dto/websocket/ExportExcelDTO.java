package com.example.sunxu_mall.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 导出成功消息 DTO (胖消息)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportExcelDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 接收用户ID
     */
    private Long userId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 下载地址
     */
    private String fileUrl;
}
