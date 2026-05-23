package com.example.prd.enums;

/**
 * 异步导出任务状态
 */
public enum ExportTaskStatus {

    PENDING("PENDING", "排队中"),
    RUNNING("RUNNING", "生成中"),
    DONE("DONE", "已完成"),
    FAIL("FAIL", "失败");

    private final String code;
    private final String label;

    ExportTaskStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
