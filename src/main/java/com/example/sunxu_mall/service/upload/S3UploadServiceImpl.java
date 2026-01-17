package com.example.sunxu_mall.service.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.example.sunxu_mall.config.props.UploadConfig;
import com.example.sunxu_mall.dto.file.FileDTO;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

/**
 * @author sunxu
 * @description AWS S3 / MinIO 上传实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "upload.mode", havingValue = "s3")
public class S3UploadServiceImpl implements UploadService {

    private final UploadConfig properties;
    private AmazonS3 amazonS3;

    @PostConstruct
    public void init() {
        UploadConfig.S3 s3Config = properties.getS3();
        BasicAWSCredentials credentials = new BasicAWSCredentials(s3Config.getAccessKey(), s3Config.getSecretKey());

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials));

        if (StrUtil.isNotBlank(s3Config.getEndpoint())) {
            // MinIO 或其他兼容S3的服务
            builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                    s3Config.getEndpoint(), s3Config.getRegion()));
        } else {
            // 标准 AWS S3
            builder.withRegion(s3Config.getRegion());
        }

        amazonS3 = builder.build();
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

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    properties.getS3().getBucket(),
                    key,
                    file.getInputStream(),
                    metadata
            );

            amazonS3.putObject(putObjectRequest);

            // 生成访问URL
            String downloadUrl;
            if (StrUtil.isNotBlank(properties.getS3().getEndpoint())) {
                // 如果是 MinIO，拼接 Endpoint + Bucket + Key
                String endpoint = properties.getS3().getEndpoint();
                if (!endpoint.endsWith("/")) endpoint += "/";
                downloadUrl = endpoint + properties.getS3().getBucket() + "/" + key;
            } else {
                // 标准 S3 URL
                URL url = amazonS3.getUrl(properties.getS3().getBucket(), key);
                downloadUrl = url.toString();
            }

            return FileDTO.builder()
                    .originalName(originalFilename)
                    .fileName(fileName)
                    .path(key)
                    .downloadUrl(downloadUrl)
                    .type(suffix)
                    .size(file.getSize())
                    .build();

        } catch (IOException e) {
            log.error("S3文件上传失败", e);
            throw new BusinessException("S3文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("S3上传异常", e);
            throw new BusinessException("S3上传异常: " + e.getMessage());
        }
    }
}
