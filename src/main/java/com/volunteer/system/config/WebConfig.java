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

    @Autowired
    private AdminInterceptor adminInterceptor;

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

        // 授权层：在认证之后执行，统一裁决 @RequiresAdmin 声明的管理员接口。
        // 顺序很重要 —— 必须先由 JwtInterceptor 解析出可信角色，这里才有得判。
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String userDir = System.getProperty("user.dir");
        String path = userDir + "/files/";

        java.io.File file = new java.io.File(path);
        if (!file.exists()) {
            path = userDir + "/2.Backend/files/";
        }
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + path);
    }
}