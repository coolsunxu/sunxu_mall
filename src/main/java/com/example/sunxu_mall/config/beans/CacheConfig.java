package com.example.sunxu_mall.config.beans;

import com.example.sunxu_mall.config.props.IpCityConfig;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 */
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final IpCityConfig ipCityConfig;

    /**
     * IP城市信息缓存名称
     */
    public static final String IP_CITY_CACHE = "ipCityCache";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 读取配置的过期时间
        long ttlSeconds = ipCityConfig.getCache().getCaffeineTtlSeconds();
        long maxSize = ipCityConfig.getCache().getCaffeineMaxSize();

        // 配置 IP 城市缓存
        cacheManager.registerCustomCache(IP_CITY_CACHE, 
                Caffeine.newBuilder()
                        .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                        .maximumSize(maxSize)
                        .recordStats()
                        .build());
                        
        return cacheManager;
    }
}
