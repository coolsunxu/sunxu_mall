package com.example.sunxu_mall.service.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
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
            throw new BusinessException("上传文件不能为空");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = FileUtil.getSuffix(originalFilename);
            String fileName = IdUtil.simpleUUID() + "." + suffix;
            
            // 按日期生成子目录: yyyyMMdd
            String datePath = DateUtil.format(new Date(), "yyyyMMdd");
            
            // 基础路径
            String basePath = properties.getLocal().getPath();
            if (StrUtil.isBlank(basePath)) {
                basePath = System.getProperty("user.dir") + "/upload/";
            }
            
            // 完整保存路径
            String fullPath = basePath + File.separator + datePath + File.separator + fileName;
            File dest = new File(fullPath);
            FileUtil.touch(dest);
            file.transferTo(dest);

            // 生成访问URL
            String domain = properties.getLocal().getDomain();
            if (StrUtil.isBlank(domain)) {
                domain = "http://localhost:8011/files"; // 默认本地访问前缀
            }
            // 确保domain末尾没有/
            if (domain.endsWith("/")) {
                domain = domain.substring(0, domain.length() - 1);
            }
            
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
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }
}
