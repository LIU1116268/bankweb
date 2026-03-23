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
        // 1. 设置响应头：告诉浏览器，接下来的数据是一个 ZIP 压缩包
        response.setContentType("application/zip");

        // 2. 设置下载的文件名：告诉浏览器弹出下载框，默认文件名为 attachments.zip
        response.setHeader("Content-Disposition", "attachment; filename=attachments.zip");

        // 3. 开启压缩流：使用 try-with-resources 自动管理资源
        // ZipOutputStream 就像一个“自动封口的压缩纸箱”，数据丢进去就会被压缩
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {

            // 4. 循环处理每一个文件
            for (File file : files) {
                // 安全检查：如果硬盘上找不到这个文件，直接跳过，防止程序崩溃
                if (!file.exists()) continue;

                // 5. 在压缩包里“占个位置”：创建一个文件夹内部的条目 (Entry)
                // file.getName() 会拿到带 10 位随机码的文件名，比如 "a1b2c3d4e5_11.sql"
                zos.putNextEntry(new ZipEntry(file.getName()));

                // 6. 读取硬盘文件：开启文件输入流，准备把硬盘里的字节搬运出来
                try (FileInputStream fis = new FileInputStream(file)) {
                    // 准备一个 1KB 的小推车（缓冲区），分批搬运数据
                    byte[] buffer = new byte[1024];
                    int len;

                    // 7. 开始搬运：只要还没读完 (fis.read != -1)，就一直读
                    while ((len = fis.read(buffer)) > 0) {
                        // 将读到的数据写进压缩流中
                        zos.write(buffer, 0, len);
                    }
                }

                // 8. 结束当前文件的打包：关闭当前的 Entry，准备处理下一个文件
                zos.closeEntry();
            }
            // 循环结束后，try-with-resources 会自动关闭 zos，并完成压缩包的最后封包
        }
    }
}
/**
 * 文件流式打包下载功能优势说明
 * 1. 即时打包传输：无需在服务器端生成临时 ZIP 文件，避免占用服务器磁盘空间，边压缩边响应输出
 * 2. 低内存占用：采用 1024 字节缓冲区逐块读写，无论文件大小，内存占用稳定且极小
 * 3. 多文件聚合下载：可自动聚合跨日期目录的分散文件，统一打包为一个 ZIP 包，提升用户下载体验与运维效率
 */