package com.example.prd.vo;

import lombok.Data;

import java.util.List;

/**
 * 状态流转结果 / 可流转状态查询结果
 */
@Data
public class PrdStatusTransitionVO {

    private String id;

    /** 流转前状态 code */
    private String fromStatus;

    /** 流转前状态中文 */
    private String fromStatusLabel;

    /** 流转后状态 code */
    private String toStatus;

    /** 流转后状态中文 */
    private String toStatusLabel;

    /** 当前记录允许流转到的下一状态列表（查询接口用） */
    private List<String> allowedNextStatuses;

    private List<String> allowedNextStatusLabels;
}
