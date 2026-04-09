package com.example.prd.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadConfig implements WebMvcConfigurer {

    /**
     * 映射本地文件路径到网络访问路径
     * 例如：访问 http://localhost:8080/files/123.jpg
     * 实际上是读取本地 D:/prd_attachments/123.jpg
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 这里的路径建议与 Controller 中的 uploadDir 保持一致
        // 注意：Windows 路径前需要加 file:///，Linux 直接加 file:
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:///D:/prd_attachments/");
    }
}