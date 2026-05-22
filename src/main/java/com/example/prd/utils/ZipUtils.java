package com.example.prd.utils;

import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ZIP 压缩下载工具
 */
public final class ZipUtils {

    private static final int BUFFER_SIZE = 1024;

    private ZipUtils() {
    }

    /**
     * 将多个文件打包为 ZIP 并写入 HTTP 响应流
     *
     * @param files    待打包的本地文件列表
     * @param response HTTP 响应（浏览器将触发下载）
     */
    public static void downloadZip(List<File> files, HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=attachments.zip");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (File file : files) {
                if (!file.exists()) {
                    continue;
                }
                zos.putNextEntry(new ZipEntry(file.getName()));
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }
}
