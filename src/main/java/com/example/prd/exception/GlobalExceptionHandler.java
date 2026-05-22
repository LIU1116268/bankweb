package com.example.prd.exception;

import com.example.prd.common.Result;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 限流超限：返回 code=429
     */
    @ExceptionHandler(RateLimitException.class)
    public Result<Void> handleRateLimit(RateLimitException e) {
        return Result.tooManyRequests(e.getMessage());
    }

    /**
     * 状态机非法流转等业务校验失败
     */
    @ExceptionHandler(PrdStatusException.class)
    public Result<Void> handlePrdStatus(PrdStatusException e) {
        return Result.error(e.getMessage());
    }

    /**
     * 数据库异常（常见：未执行 STATUS 字段迁移脚本，报 Unknown column 'STATUS'）
     */
    @ExceptionHandler(DataAccessException.class)
    public Result<Void> handleDataAccess(DataAccessException e) {
        String msg = e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage()
                : e.getMessage();
        if (msg != null && msg.contains("STATUS")) {
            msg = "数据库缺少 STATUS 字段，请先执行 sql/prd_check_list_add_status.sql：" + msg;
        }
        return Result.error("数据库操作失败：" + msg);
    }
}
