package com.example.prd.vo;

import lombok.Data;

/**
 * 异步导出任务信息
 */
@Data
public class ExportTaskVO {

    private String taskId;

    /** EXCEL / ZIP */
    private String taskType;

    /** PENDING / RUNNING / DONE / FAIL */
    private String status;

    private String statusLabel;

    /** 完成后的文件名（用于下载） */
    private String fileName;

    /** 失败原因 */
    private String message;

    /** 创建时间戳 */
    private Long createTime;

    /** 完成时间戳 */
    private Long finishTime;
}
