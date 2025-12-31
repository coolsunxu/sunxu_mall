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

    private Pool connectionPool = new Pool();

    @Data
    public static class Pool {
        private int maxIdleConnections = 32;
        private Duration keepAliveDuration = Duration.ofMinutes(5);
    }
}
