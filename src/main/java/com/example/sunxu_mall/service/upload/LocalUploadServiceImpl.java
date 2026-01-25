package com.example.sunxu_mall.service.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.example.sunxu_mall.config.props.UploadConfig;
import com.example.sunxu_mall.dto.file.FileDTO;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * @author sunxu
 * @description 本地文件上传实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "upload.mode", havingValue = "local", matchIfMissing = true)
public class LocalUploadServiceImpl implements UploadService {

    private final UploadConfig properties;

    @Override
    public FileDTO upload(MultipartFile file, String bizType, String fileType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("file cannot be empty");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = FileUtil.getSuffix(originalFilename);
            String fileName = IdUtil.simpleUUID() + "." + suffix;

            // 按日期生成子目录: yyyyMMdd
            String datePath = DateUtil.format(new Date(), "yyyyMMdd");

            // 基础路径
            String basePath = properties.getLocal().getPath();

            // 确保基础路径以分隔符结尾，避免路径拼接错误
            if (!basePath.endsWith(File.separator)) {
                basePath += File.separator;
            }

            // 完整保存路径
            String fullPath = basePath + datePath + File.separator + fileName;
            File dest = new File(fullPath);
            FileUtil.touch(dest);
            file.transferTo(dest);

            // 生成访问URL
            String domain = properties.getLocal().getDomain();
            
            String downloadUrl = domain + "/" + datePath + "/" + fileName;

            return FileDTO.builder()
                    .originalName(originalFilename)
                    .fileName(fileName)
                    .path(fullPath)
                    .downloadUrl(downloadUrl)
                    .type(suffix)
                    .size(file.getSize())
                    .build();

        } catch (IOException e) {
            log.warn("upload file failed", e);
            throw new BusinessException("upload file failed: " + e.getMessage());
        }
    }
}
