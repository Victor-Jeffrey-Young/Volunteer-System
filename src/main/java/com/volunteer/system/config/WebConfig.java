package com.volunteer.system.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**") // 拦截所有以 /api 开头的请求
                .excludePathPatterns(
                        "/api/auth/**",      // 放行登录注册
                        "/doc.html",         // 放行 Knife4j 文档
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/favicon.ico"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 🚨 增强版路径探测逻辑：兼容在项目根目录启动或在 2.Backend 目录启动
        String userDir = System.getProperty("user.dir");
        String path = userDir + "/files/";
        
        // 如果当前目录下没有 files，但存在 2.Backend/files，则自动修正
        java.io.File file = new java.io.File(path);
        if (!file.exists()) {
            path = userDir + "/2.Backend/files/";
        }

        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + path);
    }
}