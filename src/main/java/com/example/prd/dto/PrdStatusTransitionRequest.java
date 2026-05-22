package com.example.prd.dto;

import lombok.Data;

/**
 * PRD 状态流转请求体
 */
@Data
public class PrdStatusTransitionRequest {

    /** 清单主键 ID */
    private String id;

    /**
     * 目标状态 code，取值见 {@link com.example.prd.enums.PrdCheckStatus}
     * 如：SUBMITTED、UAT_PASSED、PROD_READY、ARCHIVED
     */
    private String targetStatus;

    /** 操作人（写入 update_user，并出现在审计日志 oper_param 中） */
    private String operator;

    /** 流转备注（可选，写入 remark 或仅用于审计；此处写入 remark 字段便于追溯） */
    private String remark;
}
