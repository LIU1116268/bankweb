package com.example.prd.service;

import com.example.prd.vo.ExportTaskVO;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 异步导出任务服务
 */
public interface ExportTaskService {

    /** 提交 Excel 异步导出任务，立即返回 taskId */
    ExportTaskVO submitExcelAsync(String demandName);

    /** 提交 ZIP 异步打包任务 */
    ExportTaskVO submitZipAsync(List<String> ids);

    /** 查询任务状态 */
    ExportTaskVO getTask(String taskId);

    /** 下载已完成的任务文件 */
    void downloadTask(String taskId, HttpServletResponse response) throws IOException;
}
