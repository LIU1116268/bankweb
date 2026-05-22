package com.example.prd.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * PRD 投产检查清单 - 流程状态枚举
 * <p>
 * 状态流转图（仅允许箭头方向，不可跳转、不可回退到归档之前以外的乱序状态）：
 * <pre>
 *   草稿(DRAFT) → 已提交(SUBMITTED) → UAT通过(UAT_PASSED) → 待投产(PROD_READY) → 已归档(ARCHIVED)
 *                      ↑__________________|
 *                      （仅允许退回到草稿，便于打回修改）
 * </pre>
 */
public enum PrdCheckStatus {

    DRAFT("DRAFT", "草稿"),
    SUBMITTED("SUBMITTED", "已提交"),
    UAT_PASSED("UAT_PASSED", "UAT通过"),
    PROD_READY("PROD_READY", "待投产"),
    ARCHIVED("ARCHIVED", "已归档");

    private final String code;
    private final String label;

    /** 合法流转表：当前状态 → 允许进入的下一状态集合 */
    private static final Map<PrdCheckStatus, Set<PrdCheckStatus>> ALLOWED_NEXT = Map.of(
            DRAFT, EnumSet.of(SUBMITTED),
            SUBMITTED, EnumSet.of(UAT_PASSED, DRAFT),
            UAT_PASSED, EnumSet.of(PROD_READY),
            PROD_READY, EnumSet.of(ARCHIVED),
            ARCHIVED, Collections.emptySet()
    );

    PrdCheckStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 根据数据库存储的 code 解析枚举；空或非法时返回 null
     */
    public static PrdCheckStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (PrdCheckStatus s : values()) {
            if (s.code.equalsIgnoreCase(code.trim())) {
                return s;
            }
        }
        return null;
    }

    /**
     * 是否允许从当前状态流转到目标状态
     */
    public boolean canTransitTo(PrdCheckStatus target) {
        if (target == null) {
            return false;
        }
        return ALLOWED_NEXT.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    /** 获取当前状态可流转到的下一状态（供前端下拉） */
    public Set<PrdCheckStatus> allowedNextStatuses() {
        return ALLOWED_NEXT.getOrDefault(this, Collections.emptySet());
    }
}
