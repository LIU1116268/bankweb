package com.example.prd.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式限流注解
 * <p>
 * 基于 Redisson {@code RRateLimiter}，由 {@link com.example.prd.aspect.RateLimitAspect} 在方法执行前校验
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 时间窗口内允许的最大请求数 */
    int rate() default 10;

    /** 时间窗口长度 */
    int interval() default 1;

    /** 时间窗口单位 */
    TimeUnit timeUnit() default TimeUnit.MINUTES;

    /** 限流维度（默认按 IP + 接口） */
    LimitType limitType() default LimitType.IP_METHOD;

    /** 自定义 key 后缀，用于区分同一方法下的不同限流策略 */
    String key() default "";
}
