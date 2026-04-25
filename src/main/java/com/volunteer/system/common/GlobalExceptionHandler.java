package com.volunteer.system.common;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：拦截 Controller 抛出的异常，转化为标准的 Result 返回
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获自定义的业务异常 (ServiceException)
     */
    @ExceptionHandler(ServiceException.class)
    public Result<?> handleServiceException(ServiceException e, HttpServletResponse response) {
        // 1. 后端控制台打印：包含错误码，方便排查
        log.warn("【业务异常】状态码: {}, 提示信息: {}", e.getCode(), e.getMessage());

        // 2. 同步设置 HTTP 响应状态码
        response.setStatus(e.getCode());
        
        // 3. 显式设置编码，防止 Safari 等浏览器乱码
        response.setCharacterEncoding("UTF-8");

        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 捕获未知的系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletResponse response) {
        log.error("【系统未知错误】", e);
        
        // 系统崩了通常给 500
        response.setStatus(500);
        response.setCharacterEncoding("UTF-8");

        return Result.error(500, "服务器繁忙，请联系管理员或稍后再试");
    }
}
