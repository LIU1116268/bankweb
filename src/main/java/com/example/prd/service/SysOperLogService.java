package com.example.prd.service;

import com.example.prd.entity.SysOperLog;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 系统操作审计日志 - 业务接口
 */
public interface SysOperLogService {

    /** 异步写入操作日志（由 AOP 切面调用） */
    void insertOperLog(SysOperLog operLog);

    /** 查询最近日志列表 */
    List<SysOperLog> selectLogList();

    /** 导出全部日志 Excel */
    void exportLog(HttpServletResponse response) throws IOException;

    /** 按时间范围导出日志 Excel（无时间范围时限制 1000 条） */
    void exportLog(String beginTime, String endTime, HttpServletResponse response) throws IOException;
}
