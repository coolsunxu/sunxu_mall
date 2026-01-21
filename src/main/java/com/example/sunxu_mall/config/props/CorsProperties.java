package com.example.sunxu_mall.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CORS 配置属性
 * 支持通过 application.yaml 配置跨域策略
 *
 * @author sunxu
 * @date 2026/1/24
 */
@Data
@Component
@ConfigurationProperties(prefix = "mall.cors")
public class CorsProperties {

    /**
     * 允许的来源列表
     * 默认允许本地开发端口
     */
    private List<String> allowedOrigins = List.of(
            "http://localhost:3000",
            "http://localhost:3001",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001"
    );

    /**
     * 允许的请求头
     */
    private List<String> allowedHeaders = List.of("*");

    /**
     * 允许的请求方法
     */
    private List<String> allowedMethods = List.of("*");

    /**
     * 是否允许发送 Cookie
     */
    private boolean allowCredentials = true;

    /**
     * 预检请求缓存时间（秒）
     */
    private long maxAge = 3600L;
}
