package com.example.prd.service;

import com.example.prd.entity.SysOperLog;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public interface SysOperLogService {
    /**
     * 异步保存操作日志
     */
    void insertOperLog(SysOperLog operLog);

    /**
     * 查询最近日志
     */
    List<SysOperLog> selectLogList();

    /**
     * 导出日志到 Excel
     * @param response 响应对象，用于文件下载流
     * @throws IOException 写入异常
     */
    void exportLog(HttpServletResponse response) throws IOException;

    // 导出增加时间范围限制
    void exportLog(String beginTime, String endTime, HttpServletResponse response) throws IOException;
}