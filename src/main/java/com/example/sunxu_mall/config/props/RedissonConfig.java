package com.example.sunxu_mall.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author sunxu
 */
@Data
@ConfigurationProperties(prefix = "redisson")
@Component
public class RedissonConfig {

    private String filepath;

}
