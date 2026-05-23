package com.example.prd.service.impl;

import com.example.prd.enums.ExportTaskStatus;
import com.example.prd.enums.ExportTaskType;
import com.example.prd.service.DataScopeService;
import com.example.prd.service.ExportTaskService;
import com.example.prd.vo.ExportTaskVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 异步导出任务服务（提交与查询）
 */
@Service
public class ExportTaskServiceImpl implements ExportTaskService {

    private static final String TASK_KEY_PREFIX = "bankweb:export:task:";
    private static final long TASK_TTL_HOURS = 24L;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private ExportAsyncExecutor exportAsyncExecutor;

    @Value("${file.upload-path:D:/uploads/}")
    private String uploadRootPath;

    @Override
    public ExportTaskVO submitExcelAsync(String demandName) {
        // 提交时固化机构范围，异步线程里 ThreadLocal 不可用
        List<Long> scopeDeptIds = dataScopeService.resolveDeptFilter(null, true);
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ExportTaskVO task = buildTask(taskId, ExportTaskType.EXCEL);
        saveTask(task);
        exportAsyncExecutor.runExcel(taskId, demandName, scopeDeptIds);
        return task;
    }

    @Override
    public ExportTaskVO submitZipAsync(List<String> ids) {
        dataScopeService.checkRecordIdsAccess(ids);
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ExportTaskVO task = buildTask(taskId, ExportTaskType.ZIP);
        saveTask(task);
        exportAsyncExecutor.runZip(taskId, ids);
        return task;
    }

    @Override
    public ExportTaskVO getTask(String taskId) {
        ExportTaskVO task = loadTask(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在或已过期：" + taskId);
        }
        return task;
    }

    @Override
    public void downloadTask(String taskId, HttpServletResponse response) throws IOException {
        ExportTaskVO task = getTask(taskId);
        if (!ExportTaskStatus.DONE.getCode().equals(task.getStatus())) {
            throw new RuntimeException("任务未完成，当前状态：" + task.getStatusLabel());
        }
        File file = new File(uploadRootPath + "export_tasks/" + task.getFileName());
        if (!file.exists()) {
            throw new RuntimeException("导出文件不存在或已被清理");
        }
        String contentType = task.getFileName().endsWith(".zip")
                ? "application/zip"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode(task.getFileName(), StandardCharsets.UTF_8));
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
        }
    }

    private ExportTaskVO buildTask(String taskId, ExportTaskType type) {
        ExportTaskVO vo = new ExportTaskVO();
        vo.setTaskId(taskId);
        vo.setTaskType(type.name());
        vo.setStatus(ExportTaskStatus.PENDING.getCode());
        vo.setStatusLabel(ExportTaskStatus.PENDING.getLabel());
        vo.setCreateTime(System.currentTimeMillis());
        return vo;
    }

    private void saveTask(ExportTaskVO task) {
        redisTemplate.opsForValue().set(TASK_KEY_PREFIX + task.getTaskId(), task, TASK_TTL_HOURS, TimeUnit.HOURS);
    }

    private ExportTaskVO loadTask(String taskId) {
        Object obj = redisTemplate.opsForValue().get(TASK_KEY_PREFIX + taskId);
        if (obj instanceof ExportTaskVO) {
            return (ExportTaskVO) obj;
        }
        return null;
    }
}
