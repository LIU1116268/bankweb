

package com.example.prd.annotation;

import java.lang.annotation.*;

/**
 * 自定义操作日志记录注解
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD }) // 注解用于方法
@Retention(RetentionPolicy.RUNTIME) // 运行时有效
@Documented
public @interface Log {

    String title() default "";

    String businessType() default "OTHER";
}