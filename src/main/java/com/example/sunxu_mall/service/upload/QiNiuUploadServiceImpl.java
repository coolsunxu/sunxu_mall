package com.example.sunxu_mall.service.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.example.sunxu_mall.config.props.UploadConfig;
import com.example.sunxu_mall.dto.FileDTO;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.service.UploadService;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Date;

/**
 * @author sunxu
 * @description 七牛云上传实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "upload.mode", havingValue = "qiniu")
public class QiNiuUploadServiceImpl implements UploadService {

    private final UploadConfig properties;
    private UploadManager uploadManager;
    private Auth auth;

    @PostConstruct
    public void init() {
        // 构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.autoRegion());
        // 指定分片上传版本
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
        uploadManager = new UploadManager(cfg);
        auth = Auth.create(properties.getQiniu().getAccessKey(), properties.getQiniu().getSecretKey());
    }

    @Override
    public FileDTO upload(MultipartFile file, String bizType, String fileType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = FileUtil.getSuffix(originalFilename);
            String fileName = IdUtil.simpleUUID() + "." + suffix;
            
            // 按日期生成子目录
            String datePath = DateUtil.format(new Date(), "yyyyMMdd");
            String key = datePath + "/" + fileName;

            String upToken = auth.uploadToken(properties.getQiniu().getBucket());

            Response response = uploadManager.put(file.getInputStream(), key, upToken, null, null);
            
            // 解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            
            String domain = properties.getQiniu().getDomain();
            if (!domain.endsWith("/")) {
                domain = domain + "/";
            }
            String downloadUrl = domain + putRet.key;

            // 如果是私有空间，生成带签名的下载链接
            if (properties.getQiniu().isPrivateBucket()) {
                // 默认 1 小时过期
                downloadUrl = auth.privateDownloadUrl(downloadUrl, 3600);
            }

            return FileDTO.builder()
                    .originalName(originalFilename)
                    .fileName(fileName)
                    .path(key)
                    .downloadUrl(downloadUrl)
                    .type(suffix)
                    .size(file.getSize())
                    .build();

        } catch (QiniuException ex) {
            Response r = ex.response;
            log.error("七牛云上传失败: {}", r.toString());
            throw new BusinessException("七牛云上传失败: " + r.toString());
        } catch (IOException e) {
            log.error("文件上传异常", e);
            throw new BusinessException("文件上传异常: " + e.getMessage());
        }
    }
}
