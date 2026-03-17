package com.volunteer.system.common;

import lombok.Getter;

/**
 * 自定义业务异常：用于在 Service 层主动抛出业务逻辑错误
 */
@Getter
public class ServiceException extends RuntimeException {
    private final Integer code;

    public ServiceException(String msg) {
        super(msg);
        this.code = 500;
    }

    public ServiceException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }
}
