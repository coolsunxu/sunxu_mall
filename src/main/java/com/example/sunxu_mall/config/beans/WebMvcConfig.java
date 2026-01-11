package com.example.sunxu_mall.config.beans;

import com.example.sunxu_mall.config.props.UploadConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author sunxu
 * @description Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final UploadConfig uploadConfig;

    public WebMvcConfig(UploadConfig uploadConfig) {
        this.uploadConfig = uploadConfig;
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
}
