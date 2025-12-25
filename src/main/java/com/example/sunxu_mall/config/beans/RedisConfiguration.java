package com.example.sunxu_mall.config.beans;


import com.example.sunxu_mall.config.props.RedissonConfig;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author sunxu
 */
@Slf4j
@Configuration
public class RedisConfiguration {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedissonConfig redissonConfig, ResourceLoader resourceLoader) throws IOException {
        Resource resource = resourceLoader.getResource(redissonConfig.getFilepath());
        try (InputStream in = resource.getInputStream()) {
            Config config = Config.fromYAML(in);
            return Redisson.create(config);
        }
    }

}
