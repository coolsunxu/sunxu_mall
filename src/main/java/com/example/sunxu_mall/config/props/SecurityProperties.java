package com.example.sunxu_mall.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 安全配置属性
 * 支持通过 application.yaml 配置安全策略
 *
 * @author sunxu
 * @date 2026/1/24
 */
@Data
@Component
@ConfigurationProperties(prefix = "mall.security")
public class SecurityProperties {

    /**
     * 是否放行 WebSocket 端点（不需要认证）
     * - true: 本地/测试环境，可直接连接
     * - false: 生产环境，需要 token 认证
     */
    private boolean permitWebSocket = true;

    /**
     * 额外需要放行的 URL 模式（不需要认证）
     */
    private List<String> permitUrls = List.of();
}
