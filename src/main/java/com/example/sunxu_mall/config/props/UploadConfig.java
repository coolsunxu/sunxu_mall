package com.example.sunxu_mall.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author sunxu
 * @description 文件上传配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "upload")
public class UploadConfig {
    /**
     * 上传模式: local, qiniu, s3
     */
    private String mode;

    private Local local = new Local();
    private Qiniu qiniu = new Qiniu();
    private S3 s3 = new S3();

    @Data
    public static class Local {
        /**
         * 本地存储路径
         */
        private String path;
        /**
         * 访问域名/前缀
         */
        private String domain;
    }

    @Data
    public static class Qiniu {
        private String accessKey;
        private String secretKey;
        private String bucket;
        private String domain;
    }

    @Data
    public static class S3 {
        private String accessKey;
        private String secretKey;
        private String bucket;
        private String region;
        private String endpoint;
    }
}
