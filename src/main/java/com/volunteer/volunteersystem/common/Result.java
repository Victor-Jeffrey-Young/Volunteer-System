package com.volunteer.volunteersystem.common;

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

    public static Result error(String msg) {
        Result result = new Result<>();
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }
}