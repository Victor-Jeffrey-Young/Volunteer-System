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

    @Autowired
    private UploadProperties uploadProperties;

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
        // 与 FileController 共用同一个解析结果：上传写到哪，这里就从哪读。
        // 以前两边各自拼 user.dir，容器/服务化启动（工作目录不同）时会对不上，图片就 404。
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadProperties.getRootDir());
    }
}