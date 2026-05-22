package com.example.prd.annotation;

/**
 * 分布式限流维度（配合 {@link RateLimit#limitType()} 使用）
 * <p>
 * 决定 Redisson 限流器在 Redis 中的 Key 如何拼接
 */
public enum LimitType {

    /**
     * 按接口全局限流：所有用户共享同一把限流器
     * <p>
     * Key 示例：{@code bankweb:rateLimit:PrdCheckListController.exportZip}
     */
    METHOD,

    /**
     * 按客户端 IP 限流：同一 IP 访问任何带限流的接口都共用配额（慎用）
     * <p>
     * Key 示例：{@code bankweb:rateLimit:192.168.1.100}
     */
    IP,

    /**
     * 按「接口 + IP」组合限流（默认推荐）
     * <p>
     * 不同接口互不影响，同一接口下不同 IP 各自计数，最适合防刷上传/导出
     * <p>
     * Key 示例：{@code bankweb:rateLimit:PrdCheckListController.upload:upload:127.0.0.1}
     */
    IP_METHOD
}
