package com.example.sunxu_mall.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 异步线程池配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "mall.mgt.async")
public class ThreadPoolConfig {
    /**
     * 线程池配置映射，key为业务名称（如 login）
     */
    private Map<String, PoolConfig> pools;

    @Data
    public static class PoolConfig {
        private int coreSize = 2;
        private int maxSize = 5;
        private int queueCapacity = 100;
        private int keepAliveSeconds = 60;
        private String threadNamePrefix = "async-";
    }
}
