package com.example.prd.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 附件静态资源映射
 * <p>
 * 将本地磁盘路径映射为 HTTP 访问路径，便于预览已上传附件
 */
@Configuration
public class UploadConfig implements WebMvcConfigurer {

    /**
     * 访问示例：http://localhost:8080/files/2026/05/21/xxx.pdf
     * <p>
     * 对应本地：D:/prd_attachments/2026/05/21/xxx.pdf（需与 application.yml 中 file.upload-path 保持一致）
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:///D:/prd_attachments/");
    }
}
