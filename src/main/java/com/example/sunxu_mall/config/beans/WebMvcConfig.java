package com.example.sunxu_mall.config.beans;

import com.example.sunxu_mall.config.props.UploadConfig;
import com.example.sunxu_mall.idempotency.IdempotencyProperties;
import com.example.sunxu_mall.interceptor.IdempotencyInterceptor;
import com.example.sunxu_mall.interceptor.PerformanceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author sunxu
 * @description Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 默认排除的路径 */
    private static final List<String> DEFAULT_EXCLUDE_PATHS = Arrays.asList(
            "/swagger-ui/**", "/v3/api-docs/**", "/files/**"
    );

    private final UploadConfig uploadConfig;
    private final PerformanceInterceptor performanceInterceptor;
    private final IdempotencyInterceptor idempotencyInterceptor;
    private final IdempotencyProperties idempotencyProperties;

    public WebMvcConfig(UploadConfig uploadConfig,
                        PerformanceInterceptor performanceInterceptor,
                        IdempotencyInterceptor idempotencyInterceptor,
                        IdempotencyProperties idempotencyProperties) {
        this.uploadConfig = uploadConfig;
        this.performanceInterceptor = performanceInterceptor;
        this.idempotencyInterceptor = idempotencyInterceptor;
        this.idempotencyProperties = idempotencyProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(performanceInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**", "/files/**");

        // 幂等拦截器：对所有写接口生效，排除静态资源和文档路径
        List<String> excludePaths = buildExcludePaths();
        registry.addInterceptor(idempotencyInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(excludePaths.toArray(new String[0]));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射本地文件上传路径
        String path = uploadConfig.getLocal().getPath();
        // 确保路径以 / 结尾
        if (!path.endsWith("/")) {
            path += "/";
        }
        
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + path);
    }

    /**
     * 合并默认排除路径与配置文件中的自定义排除路径
     */
    private List<String> buildExcludePaths() {
        List<String> paths = new ArrayList<>(DEFAULT_EXCLUDE_PATHS);
        List<String> customPaths = idempotencyProperties.getExcludePaths();
        if (customPaths != null && !customPaths.isEmpty()) {
            paths.addAll(customPaths);
        }
        return paths;
    }
}
