package com.example.prd.exception;

/**
 * PRD 状态机校验失败时抛出（非法流转、记录不存在等）
 */
public class PrdStatusException extends RuntimeException {

    public PrdStatusException(String message) {
        super(message);
    }
}
