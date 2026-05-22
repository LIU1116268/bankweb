package com.example.prd.common;

import lombok.Data;

/**
 * 统一 API 响应封装
 * <p>
 * 约定：code=200 成功，code=500 业务失败，code=429 触发限流
 */
@Data
public class Result<T> {

    /** 业务状态码 */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 业务数据 */
    private T data;

    // ==================== 成功 ====================

    public static <T> Result<T> success(T data) {
        return success("操作成功", data);
    }

    public static Result<Void> success() {
        return success("操作成功", null);
    }

    public static <T> Result<T> success(String msg, T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = msg;
        r.data = data;
        return r;
    }

    // ==================== 失败 ====================

    public static <T> Result<T> error(String msg) {
        return error(500, msg);
    }

    public static <T> Result<T> error() {
        return error("操作失败");
    }

    public static <T> Result<T> error(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        r.data = null;
        return r;
    }

    /** 请求过于频繁（对应 HTTP 429 语义） */
    public static <T> Result<T> tooManyRequests(String msg) {
        return error(429, msg);
    }
}
