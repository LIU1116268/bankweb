package com.example.prd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@EnableAsync
@EnableAspectJAutoProxy
@SpringBootApplication
public class BankwebApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankwebApplication.class, args);
    }
}