package com.example.prd.annotation;

import java.lang.annotation.*;

/**
 * 操作审计日志注解
 * <p>
 * 标注在 Controller 方法上，由 {@link com.example.prd.aspect.LogAspect} 拦截并异步写入 sys_oper_log
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /** 操作模块名称 */
    String title() default "";

    /** 业务类型，如 SAVE、UPLOAD、DELETE */
    String businessType() default "OTHER";
}
