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

    /** Redis 中限流器 Key 的统一前缀 */
    private static final String KEY_PREFIX = "bankweb:rateLimit:";

    /** 注入 Redisson，使用 RRateLimiter 实现分布式令牌桶限流 */
    @Resource
    private RedissonClient redissonClient;

    @Pointcut("@annotation(com.example.prd.annotation.RateLimit)")
    public void rateLimitPointCut() {
    }

    /**
     * 在 Controller 方法执行前先申请令牌；拿不到则抛异常，由全局处理器返回 429
     */
    @Around("rateLimitPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        // 按「类.方法:key:IP」拼 Redis 中的限流器名称
        String limitKey = buildLimitKey(joinPoint, rateLimit);
        RRateLimiter limiter = redissonClient.getRateLimiter(limitKey);
        initRateLimiter(limiter, rateLimit);

        // tryAcquire(1)：申请 1 个令牌，失败说明当前窗口配额已用完
        if (!limiter.tryAcquire(1)) {
            throw new RateLimitException("访问过于频繁，请稍后再试");
        }
        return joinPoint.proceed();
    }

    /** 首次使用时初始化速率；已存在配置则不再重复设置 */
    private void initRateLimiter(RRateLimiter limiter, RateLimit rateLimit) {
        // 检查限流器是否已存在且配置有效
        if (limiter.isExists()) {
            // 如果已存在，尝试获取配置验证其有效性
            try {
                var config = limiter.getConfig();
                if (config != null) {
                    // 配置有效，无需重新初始化
                    return;
                }
            } catch (Exception e) {
                // 配置可能损坏，需要重新初始化
                System.err.println("RateLimiter配置损坏，重新初始化: " + e.getMessage());
            }
        }
        
        // 限流器不存在或配置无效，重新初始化
        try {
            // 先删除可能存在的旧数据
            limiter.delete();
            // 使用 trySetRate 初始化
            limiter.trySetRate(
                    RateType.OVERALL,
                    rateLimit.rate(),
                    rateLimit.interval(),
                    toRateIntervalUnit(rateLimit.timeUnit())
            );
        } catch (Exception e) {
            System.err.println("RateLimiter初始化失败: " + e.getMessage());
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

    /**
     * 解析客户端 IP（兼容 Nginx 反向代理）
     * 优先 X-Forwarded-For 第一个 IP，其次 X-Real-IP，最后 RemoteAddr
     */
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
