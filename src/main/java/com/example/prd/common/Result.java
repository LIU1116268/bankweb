package com.example.prd.common;

import lombok.Data;

/**
 * 统一响应结果实体类
 * 用于前后端交互的数据封装
 */
@Data
public class Result<T> {
    private int code;   // 业务状态码：200 成功，500 失败
    private String msg; // 提示消息
    private T data;     // 承载的具体数据

    /**
     * 成功：仅返回数据，使用默认提示语
     */
    public static <T> Result<T> success(T data) {
        return success("操作成功", data);
    }

    /**
     * 成功：不返回数据（如删除、更新成功）
     */
    public static Result success() {
        return success("操作成功", null);
    }

    /**
     * 成功：自定义提示语 + 返回数据 (解决 Controller 报错的关键)
     */
    public static <T> Result<T> success(String msg, T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = msg;
        r.data = data;
        return r;
    }

    /**
     * 失败：自定义错误信息
     */
    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.code = 500;
        r.msg = msg;
        r.data = null;
        return r;
    }

    /**
     * 失败：使用默认错误提示
     */
    public static <T> Result<T> error() {
        return error("操作失败");
    }
}