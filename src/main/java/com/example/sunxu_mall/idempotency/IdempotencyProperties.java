package com.example.sunxu_mall.idempotency;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 幂等/防重复提交配置
 *
 * @author sunxu
 */
@Data
@Component
@ConfigurationProperties(prefix = "mall.idempotency")
public class IdempotencyProperties {

    /**
     * 是否启用幂等拦截（全局开关）
     */
    private boolean enabled = true;

    /**
     * 客户端幂等 Header 名称
     */
    private String headerName = "Idempotency-Key";

    /**
     * 默认幂等窗口时间（秒），同一请求在窗口内不允许重复提交
     */
    private int defaultTtlSeconds = 10;

    /**
     * Redis key 前缀
     */
    private String keyPrefix = "idem:";

    /**
     * 需要排除的路径模式列表（Ant 风格）
     */
    private List<String> excludePaths = new ArrayList<>();
}
