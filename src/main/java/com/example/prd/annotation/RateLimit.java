package com.example.prd.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson 的分布式限流注解
 * <p>
 * 标注在 Controller 方法上，由 {@link com.example.prd.aspect.RateLimitAspect} 在方法执行前拦截。
 * 底层使用 Redisson 的 {@code RRateLimiter}，配额在 Redis 集群内共享，多实例部署也生效。
 * <p>
 * 使用示例：
 * <pre>
 * // 同一 IP 对 list 接口：每分钟最多 60 次
 * {@code @RateLimit(rate = 60, interval = 1, key = "list")}
 * {@code @GetMapping("/list")}
 * </pre>
 * <p>
 * 超限后抛出 {@link com.example.prd.exception.RateLimitException}，
 * 由全局异常处理返回 {@code code=429}。
 */
@Target(ElementType.METHOD)              // 只能贴在方法上（一般贴在 Controller 接口方法）
@Retention(RetentionPolicy.RUNTIME)     // 运行时保留，AOP 才能通过反射读到
@Documented
public @interface RateLimit {

    /**
     * 时间窗口内允许的最大请求次数（令牌数）
     * <p>
     * 例如 rate=10、interval=1、timeUnit=MINUTES 表示每分钟最多 10 次
     */
    int rate() default 10;

    /**
     * 时间窗口的长度（与 timeUnit 配合）
     */
    int interval() default 1;

    /**
     * 时间窗口的单位，默认分钟
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;

    /**
     * 限流维度，默认 {@link LimitType#IP_METHOD}（按 IP + 接口组合，防刷更精细）
     */
    LimitType limitType() default LimitType.IP_METHOD;

    /**
     * 自定义 key 后缀，会拼进 Redis 限流器名称
     * <p>
     * 完整 Key 示例：{@code bankweb:rateLimit:PrdCheckListController.list:list:127.0.0.1}
     * <p>
     * 同一方法若需区分不同业务场景，可通过 key 拆开限流桶
     */
    String key() default "";
}
