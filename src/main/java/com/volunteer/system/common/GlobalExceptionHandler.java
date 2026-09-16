package com.volunteer.system.common;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
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
     * 捕获悲观锁冲突：死锁被数据库选中回滚（MySQL 1213）或锁等待超时（1205）。
     *
     * Spring 会把这两个错误码分别翻译成 DeadlockLoserDataAccessException 与
     * CannotAcquireLockException，二者同属 PessimisticLockingFailureException 家族，
     * 所以一个分支就能覆盖（映射关系见 spring-jdbc 的 sql-error-codes.xml、
     * 继承关系见 spring-tx 6.1.6）。
     *
     * 这类失败的特点是「数据没写坏，只是这一瞬间抢不到锁」，属于可重试的冲突而非系统故障，
     * 因此返回 409 + 可理解的提示，而不是落进兜底的 500「服务器繁忙」——
     * 让用户与排查者都能一眼区分「操作太频繁」和「系统真的出问题了」。
     *
     * 刻意不做自动重试：盲目重试会把真实的锁竞争掩盖成「偶发变慢」，
     * 让本该暴露的设计问题消失；要重试也应由调用方针对幂等操作显式决定。
     * 原始异常用 warn 记录（含具体 SQL 报错），方便定位是哪条语句在争锁。
     */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public Result<?> handlePessimisticLockingFailure(PessimisticLockingFailureException e,
                                                     HttpServletResponse response) {
        log.warn("【锁冲突】未能获取行锁，按可重试冲突返回 409: {}", e.getMessage());

        response.setStatus(409);
        response.setCharacterEncoding("UTF-8");

        return Result.error(409, "当前操作较为频繁，请稍后重试");
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
