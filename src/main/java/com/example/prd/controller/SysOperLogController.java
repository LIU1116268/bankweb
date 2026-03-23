package com.example.prd.controller; // 补齐了包名

import com.example.prd.common.Result;
import com.example.prd.service.SysOperLogService; // 改为调用 Service
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


@RestController
@RequestMapping("/sysLog")
public class SysOperLogController {

    @Autowired
    private SysOperLogService operLogService;

    /**
     * 查询系统操作日志列表
     * URL: http://localhost:8080/sysLog/list
     */
    @GetMapping("/list")
    public Result list() {
        return Result.success(operLogService.selectLogList());
    }

    /**
     * 导出操作日志 Excel
     * URL: http://localhost:8080/sysLog/export  默认最近7天
     * http://localhost:8080/sysLog/export?beginTime=2026-03-01&endTime=2026-03-22
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