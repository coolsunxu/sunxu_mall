package com.example.sunxu_mall.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/31 18:35
 * @description
 */

@Data
@ConfigurationProperties(prefix = "ok.http")
public class OkHttpConfig {
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout  = Duration.ofSeconds(10);
    private Duration writeTimeout = Duration.ofSeconds(10);
    private boolean followRedirects = true;
    private boolean followSslRedirects = true;
    /**
     * 是否启用“不安全模式”：信任所有证书与域名。
     * 仅建议在本地开发/联调环境临时开启，生产环境务必保持为 false。
     *
     * 配置项：ok.http.insecure-trust-all=true|false
     */
    private boolean insecureTrustAll = false;

    private Pool connectionPool = new Pool();

    @Data
    public static class Pool {
        private int maxIdleConnections = 32;
        private Duration keepAliveDuration = Duration.ofMinutes(5);
    }
}
