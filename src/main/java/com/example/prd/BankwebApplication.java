package com.example.prd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 银行投产材料管理系统 - 启动类
 * <p>
 * 启用能力：
 * <ul>
 *   <li>{@link EnableAsync}：操作日志异步落库</li>
 *   <li>{@link EnableAspectJAutoProxy}：审计日志、分布式限流切面</li>
 * </ul>
 */
@SpringBootApplication
@EnableAsync
@EnableAspectJAutoProxy
public class BankwebApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankwebApplication.class, args);
    }
}
