package com.volunteer.system.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code; // 200成功，500失败
    private String msg;   // 提示信息
    private T data;       // 数据本体

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(int code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}