package com.example.prd.exception;

/**
 * 分布式限流触发异常
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
