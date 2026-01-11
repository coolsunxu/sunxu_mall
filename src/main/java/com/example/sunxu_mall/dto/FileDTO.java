package com.example.sunxu_mall.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author sunxu
 * @description 文件上传响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 文件原名
     */
    private String originalName;

    /**
     * 文件存储名称
     */
    private String fileName;

    /**
     * 文件路径/Key
     */
    private String path;

    /**
     * 访问地址
     */
    private String downloadUrl;

    /**
     * 文件类型
     */
    private String type;

    /**
     * 文件大小
     */
    private Long size;
}
