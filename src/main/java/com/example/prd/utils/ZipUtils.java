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

        response.setHeader("Content-Disposition", "attachment; filename=attachments.zip");
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {

            // 4. 循环处理每一个文件
            for (File file : files) {
                if (!file.exists()) continue;

                // 5. 在压缩包里“占个位置”：创建一个文件夹内部的条目 (Entry)
                // file.getName() 会拿到带 10 位随机码的文件名，比如 "a1b2c3d4e5_11.sql"
                zos.putNextEntry(new ZipEntry(file.getName()));


                try (FileInputStream fis = new FileInputStream(file)
                     // 把硬盘上的文件，转换成一个文件输入流
                ) {
                    // 准备一个 1KB 的小推车（缓冲区），分批搬运数据
                    byte[] buffer = new byte[1024];
                    // 保存每次读到了多少字节
                    int len;

                    // 开始搬运：只要还没读完,也就是len>0 (fis.read != -1)，就一直读
                    // 从文件输入流里读数据 → 放进 buffer 数组里！
                    while ((len = fis.read(buffer)) > 0) {
                        // 将读到的数据写进输出压缩流中,0~len 规定长度
                        // zos 这个压缩流，底层直接连到浏览器
                        zos.write(buffer, 0, len);
                    }
                }
                // 8. 结束当前文件的打包：关闭当前的 Entry，准备处理下一个文件
                zos.closeEntry();
            }
        }
    }
}
