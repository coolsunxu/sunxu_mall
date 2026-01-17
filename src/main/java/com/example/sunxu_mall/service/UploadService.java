package com.example.sunxu_mall.service;

import com.example.sunxu_mall.dto.file.FileDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author sunxu
 * @description 文件上传接口
 */
public interface UploadService {
    /**
     * 上传文件
     *
     * @param file     文件
     * @param bizType  业务类型
     * @param fileType 文件类型
     * @return 文件信息
     */
    FileDTO upload(MultipartFile file, String bizType, String fileType);
}
