package com.volunteer.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射本地文件目录到 Web 访问路径
        // file: 表示文件系统路径
        String path = System.getProperty("user.dir") + "/files/";

        // 关键配置：访问 http://localhost:8080/files/xxx.png -> 映射到 本地 /files/xxx.png
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + path);
    }
}