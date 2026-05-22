package com.example.prd.utils;

import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 压缩包工具类
 */
public class ZipUtils {

    /**
     * 将文件列表打包成 ZIP 并通过浏览器下载
     *
     * @param files    待下载的文件对象列表 (File 对象)
     * @param response HttpServletResponse 对象，用于向浏览器写出数据
     * @throws IOException 读写文件可能产生的异常
     */
    public static void downloadZip(List<File> files, HttpServletResponse response) throws IOException {
        // 1. 设置响应头：告诉浏览器，接下来的数据是一个 ZIP 压缩包
        response.setContentType("application/zip");
        // Content-Disposition: attachment 表示以附件形式下载，而不是在浏览器里直接打开
        response.setHeader("Content-Disposition", "attachment; filename=attachments.zip");

        // 2. 创建 ZIP 输出流，底层直接连到 response.getOutputStream()（浏览器下载通道）
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {

            // 3. 循环处理每一个待打包文件
            for (File file : files) {
                if (!file.exists()) {
                    continue;
                }

                // 4. 在压缩包里“占个位置”：创建一个 ZIP 内部的条目 (Entry)
                // file.getName() 会拿到带随机码前缀的文件名，例如 a1b2c3d4e5_11.sql
                zos.putNextEntry(new ZipEntry(file.getName()));

                // 5. 把硬盘上的文件转成输入流，分批读入内存
                try (FileInputStream fis = new FileInputStream(file)) {
                    // 准备一个 1KB 的小缓冲区，分批搬运，避免大文件一次性读入内存
                    byte[] buffer = new byte[1024];
                    int len; // 保存每次实际读到的字节数

                    // 6. 只要还没读完（len > 0），就继续从文件流读到 buffer，再写入压缩流
                    while ((len = fis.read(buffer)) > 0) {
                        // 写入 zos：从 buffer 的 0 位置开始，写 len 个字节
                        zos.write(buffer, 0, len);
                    }
                }
                // 7. 当前文件打包结束，关闭 Entry，继续下一个文件
                zos.closeEntry();
            }
        }
    }
}
