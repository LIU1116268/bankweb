package com.example.prd.service.impl;

import com.alibaba.excel.EasyExcel;
import com.example.prd.entity.PrdCheckList;
import com.example.prd.enums.ExportTaskStatus;
import com.example.prd.enums.PrdCheckStatus;
import com.example.prd.mapper.PrdCheckListMapper;
import com.example.prd.utils.ZipUtils;
import com.example.prd.vo.ExportTaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 异步导出执行器（独立 Bean，保证 @Async 生效）
 */
@Component
public class ExportAsyncExecutor {

    private static final String TASK_KEY_PREFIX = "bankweb:export:task:";
    private static final long TASK_TTL_HOURS = 24L;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PrdCheckListMapper prdMapper;

    @Value("${file.upload-path:D:/uploads/}")
    private String uploadRootPath;

    @Async
    public void runExcel(String taskId, String demandName, List<Long> scopeDeptIds) {
        ExportTaskVO task = loadTask(taskId);
        if (task == null) {
            return;
        }
        markRunning(task);
        try {
            List<PrdCheckList> data = prdMapper.selectAllForExport(demandName, scopeDeptIds);
            for (PrdCheckList item : data) {
                item.setUatEnvCheck(transferCheckFlag(item.getUatEnvCheck()));
                item.setProdEnvCheck(transferCheckFlag(item.getProdEnvCheck()));
                PrdCheckStatus status = PrdCheckStatus.fromCode(item.getStatus());
                if (status != null) {
                    item.setStatus(status.getLabel());
                }
            }
            String fileName = "PRD_Export_" + taskId + ".xlsx";
            File outFile = ensureExportFile(fileName);
            EasyExcel.write(outFile, PrdCheckList.class).sheet("PRD核对清单").doWrite(data);
            markDone(task, fileName, null);
        } catch (Exception e) {
            markFail(task, e.getMessage());
        }
    }

    @Async
    public void runZip(String taskId, List<String> ids) {
        ExportTaskVO task = loadTask(taskId);
        if (task == null) {
            return;
        }
        markRunning(task);
        try {
            List<File> allFiles = new java.util.ArrayList<>();
            for (String id : ids) {
                PrdCheckList prd = prdMapper.selectByPrimaryKey(id);
                if (prd == null || prd.getAttachmentPath() == null) {
                    continue;
                }
                for (String p : prd.getAttachmentPath().split(",")) {
                    File f = new File(uploadRootPath + p);
                    if (f.exists()) {
                        allFiles.add(f);
                    }
                }
            }
            String fileName = "attachments_" + taskId + ".zip";
            File zipFile = ensureExportFile(fileName);
            ZipUtils.packToFile(allFiles, zipFile);
            markDone(task, fileName, null);
        } catch (Exception e) {
            markFail(task, e.getMessage());
        }
    }

    private File ensureExportFile(String fileName) {
        File dir = new File(uploadRootPath + "export_tasks/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, fileName);
    }

    private void markRunning(ExportTaskVO task) {
        task.setStatus(ExportTaskStatus.RUNNING.getCode());
        task.setStatusLabel(ExportTaskStatus.RUNNING.getLabel());
        saveTask(task);
    }

    private void markDone(ExportTaskVO task, String fileName, String message) {
        task.setStatus(ExportTaskStatus.DONE.getCode());
        task.setStatusLabel(ExportTaskStatus.DONE.getLabel());
        task.setFileName(fileName);
        task.setMessage(message);
        task.setFinishTime(System.currentTimeMillis());
        saveTask(task);
    }

    private void markFail(ExportTaskVO task, String message) {
        task.setStatus(ExportTaskStatus.FAIL.getCode());
        task.setStatusLabel(ExportTaskStatus.FAIL.getLabel());
        task.setMessage(message);
        task.setFinishTime(System.currentTimeMillis());
        saveTask(task);
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

    private String transferCheckFlag(String value) {
        if ("1".equals(value)) {
            return "通过";
        }
        if ("0".equals(value)) {
            return "未通过";
        }
        return value;
    }
}
