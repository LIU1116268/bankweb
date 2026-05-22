package com.example.prd.annotation;

/**
 * 限流维度枚举
 */
public enum LimitType {

    /** 按接口全局限流（所有客户端共享配额） */
    METHOD,

    /** 按客户端 IP 限流 */
    IP,

    /** 按 IP + 接口组合限流（默认，防刷更精细） */
    IP_METHOD
}
