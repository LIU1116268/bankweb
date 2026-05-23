package com.example.prd.exception;

/**
 * 机构数据权限校验失败（越权访问其他机构数据）
 */
public class DataScopeException extends RuntimeException {

    public DataScopeException(String message) {
        super(message);
    }
}
