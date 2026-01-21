package com.example.sunxu_mall.config.beans;

import com.example.sunxu_mall.config.props.CorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS配置类
 * 解决前端跨域请求问题
 * 支持通过 application.yaml 配置跨域策略
 * 
 * @author sunxu
 * @date 2025/12/28
 */
@Configuration
public class CorsConfig {

    private final CorsProperties corsProperties;

    public CorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许的来源（从配置读取）
        corsProperties.getAllowedOrigins().forEach(config::addAllowedOrigin);
        
        // 允许的请求头（从配置读取）
        corsProperties.getAllowedHeaders().forEach(config::addAllowedHeader);
        
        // 允许的请求方法（从配置读取）
        corsProperties.getAllowedMethods().forEach(config::addAllowedMethod);
        
        // 是否允许发送Cookie（从配置读取）
        config.setAllowCredentials(corsProperties.isAllowCredentials());
        
        // 设置预检请求缓存时间（从配置读取）
        config.setMaxAge(corsProperties.getMaxAge());
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
