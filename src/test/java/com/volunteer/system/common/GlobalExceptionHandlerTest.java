package com.volunteer.system.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.mock.web.MockHttpServletResponse;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 全局异常处理器对「锁冲突」的翻译测试。
 *
 * 纯单元测试：直接调用 handler 方法，不启 Spring 上下文。
 * MySQL 1205（锁等待超时）/ 1213（死锁被选中回滚）经 Spring 翻译后的类型是
 * CannotAcquireLockException 与 DeadlockLoserDataAccessException，
 * 二者都继承 PessimisticLockingFailureException —— 这里把这条映射钉住。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("死锁被选中回滚 → 409 友好提示，而不是 500")
    void deadlockBecomesConflict() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        Result<?> result = handler.handlePessimisticLockingFailure(
                new DeadlockLoserDataAccessException("deadlock",
                        new SQLException("Deadlock found when trying to get lock; try restarting transaction")),
                response);

        assertEquals(409, result.getCode());
        assertEquals("当前操作较为频繁，请稍后重试", result.getMsg());
        assertEquals(409, response.getStatus(), "HTTP 状态码也要是 409");
    }

    @Test
    @DisplayName("锁等待超时 → 409 友好提示，而不是 500")
    void lockWaitTimeoutBecomesConflict() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        Result<?> result = handler.handlePessimisticLockingFailure(
                new CannotAcquireLockException("lock wait timeout",
                        new SQLException("Lock wait timeout exceeded; try restarting transaction")),
                response);

        assertEquals(409, result.getCode());
        assertEquals("当前操作较为频繁，请稍后重试", result.getMsg());
        assertEquals(409, response.getStatus());
    }

    @Test
    @DisplayName("响应编码显式设为 UTF-8（Safari 等浏览器不乱码）")
    void setsUtf8Encoding() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handlePessimisticLockingFailure(
                new CannotAcquireLockException("lock wait timeout"), response);

        assertEquals("UTF-8", response.getCharacterEncoding());
    }
}
