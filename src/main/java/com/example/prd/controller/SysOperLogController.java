package com.example.prd.controller;

import com.example.prd.common.Result;
import com.example.prd.entity.SysOperLog;
import com.example.prd.service.SysOperLogService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * 系统操作审计日志 - 接口层
 * <p>
 * 日志由 {@link com.example.prd.aspect.LogAspect} 在带 {@link com.example.prd.annotation.Log} 的接口执行后自动写入
 * <p>
 * 基础地址：{@code http://localhost:8080/sysLog}
 */
@RestController
@RequestMapping("/sysLog")
public class SysOperLogController {

    @Autowired
    private SysOperLogService operLogService;

    /**
     * 查询最近操作日志（最多 50 条，按时间倒序）
     * <p>
     * 请求：GET /sysLog/list
     * <p>
     * 测试用例：
     * <pre>
     * GET http://localhost:8080/sysLog/list
     * </pre>
     */
    @GetMapping("/list")
    public Result<List<SysOperLog>> list() {
        return Result.success(operLogService.selectLogList());
    }

    /**
     * 导出操作日志 Excel
     * <p>
     * 请求：GET /sysLog/export
     * <p>
     * 参数：beginTime、endTime（可选，格式 yyyy-MM-dd）；不传时间则最多导出 1000 条
     * <p>
     * 测试用例：
     * <pre>
     * 1. 默认（最近 1000 条）：
     *    GET http://localhost:8080/sysLog/export
     *
     * 2. 按时间范围：
     *    GET http://localhost:8080/sysLog/export?beginTime=2026-03-01&amp;endTime=2026-03-22
     * </pre>
     */
    @GetMapping("/export")
    public void export(String beginTime, String endTime, HttpServletResponse response) {
        try {
            operLogService.exportLog(beginTime, endTime, response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
