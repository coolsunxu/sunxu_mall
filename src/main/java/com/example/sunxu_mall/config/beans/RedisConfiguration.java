package com.example.sunxu_mall.config.beans;


import com.example.sunxu_mall.config.props.RedissonConfig;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author sunxu
 */
@Slf4j
@Configuration
public class RedisConfiguration {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedissonConfig redissonConfig,
                                         ResourceLoader resourceLoader,
                                         Environment environment) throws IOException {
        String filePath = Objects.requireNonNull(redissonConfig.getFilepath(), "redisson.filepath must not be null");
        Resource resource = resourceLoader.getResource(filePath);
        try (InputStream in = resource.getInputStream()) {
            String yamlContent = StreamUtils.copyToString(in, Objects.requireNonNull(StandardCharsets.UTF_8));
            String resolvedYaml = environment.resolvePlaceholders(yamlContent);
            Config config = Config.fromYAML(resolvedYaml);
            return Redisson.create(config);
        }
    }

}
