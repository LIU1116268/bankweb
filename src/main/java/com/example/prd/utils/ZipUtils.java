package com.example.prd.utils;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 压缩包工具类
 */
public class ZipUtils {

    /**
     * 将文件列表打包成 ZIP 并通过浏览器下载
     * @param files    待下载的文件对象列表 (File 对象)
     * @param response HttpServletResponse 对象，用于向浏览器写出数据
     * @throws IOException 读写文件可能产生的异常
     */
    public static void downloadZip(List<File> files, HttpServletResponse response) throws IOException {
        // 设置响应头：告诉浏览器，接下来的数据是一个 ZIP 压缩包
        response.setContentType("application/zip");
        // 内容处理方式 附件下载
        response.setHeader("Content-Disposition", "attachment; filename=attachments.zip");
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (File file : files) {
                if (!file.exists()) continue;
                // 在 ZIP 包里面创建一个新的文件条目，相当于在压缩包中给这个文件占一个位置
                zos.putNextEntry(new ZipEntry(file.getName()));

                // 把硬盘上的文件，转换成一个文件输入流
                try (FileInputStream fis = new FileInputStream(file)
                ) {
                    // 创建一个 1KB 的缓冲区，用来分批搬运文件数据,避免一次性把大文件全部读进内存。
                    byte[] buffer = new byte[1024];
                    // 保存每次读到了多少字节
                    int len;

                    while ((len = fis.read(buffer)) > 0) {
                        // 将读到的数据写进输出压缩流中,0~len 规定长度
                        zos.write(buffer, 0, len);
                    }
                }
                // 关闭当前的 Entry，准备处理下一个文件
                zos.closeEntry();
            }
        }
    }
}
