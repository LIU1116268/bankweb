package com.example.prd.aspect;

import com.example.prd.annotation.LimitType;
import com.example.prd.annotation.RateLimit;
import com.example.prd.exception.RateLimitException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 分布式限流切面
 * <p>
 * 执行顺序 {@link Order#value()} = 1，优先于业务方法与审计日志切面
 */
@Aspect
@Component
@Order(1)
public class RateLimitAspect {

    private static final String KEY_PREFIX = "bankweb:rateLimit:";

    @Resource
    private RedissonClient redissonClient;

    @Pointcut("@annotation(com.example.prd.annotation.RateLimit)")
    public void rateLimitPointCut() {
    }

    @Around("rateLimitPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        String limitKey = buildLimitKey(joinPoint, rateLimit);
        RRateLimiter limiter = redissonClient.getRateLimiter(limitKey);
        initRateLimiter(limiter, rateLimit);

        if (!limiter.tryAcquire(1)) {
            throw new RateLimitException("访问过于频繁，请稍后再试");
        }
        return joinPoint.proceed();
    }

    private void initRateLimiter(RRateLimiter limiter, RateLimit rateLimit) {
        if (limiter.getConfig() == null) {
            limiter.trySetRate(
                    RateType.OVERALL,
                    rateLimit.rate(),
                    rateLimit.interval(),
                    toRateIntervalUnit(rateLimit.timeUnit())
            );
        }
    }

    private String buildLimitKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String base = joinPoint.getTarget().getClass().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        if (StringUtils.hasText(rateLimit.key())) {
            base = base + ":" + rateLimit.key();
        }

        LimitType limitType = rateLimit.limitType();
        if (limitType == LimitType.METHOD) {
            return KEY_PREFIX + base;
        }

        String clientIp = resolveClientIp();
        if (limitType == LimitType.IP) {
            return KEY_PREFIX + clientIp;
        }
        return KEY_PREFIX + base + ":" + clientIp;
    }

    private String resolveClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();

        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip)) {
            int commaIndex = ip.indexOf(',');
            return commaIndex > 0 ? ip.substring(0, commaIndex).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private RateIntervalUnit toRateIntervalUnit(TimeUnit timeUnit) {
        return switch (timeUnit) {
            case SECONDS -> RateIntervalUnit.SECONDS;
            case MINUTES -> RateIntervalUnit.MINUTES;
            case HOURS -> RateIntervalUnit.HOURS;
            case DAYS -> RateIntervalUnit.DAYS;
            default -> RateIntervalUnit.SECONDS;
        };
    }
}
