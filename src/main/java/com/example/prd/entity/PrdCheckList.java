package com.example.prd.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PRD 投产检查清单 - 实体
 * <p>
 * 对应表：prd_check_list
 */
@Data
@TableName("prd_check_list")
@HeadRowHeight(25)
@ContentRowHeight(20)
@ColumnWidth(20)
public class PrdCheckList {

    @TableId(type = IdType.ASSIGN_ID)
    @ExcelProperty("数据ID")
    private String id;

    @ExcelProperty("投产窗口版本")
    private String windowVerId;

    @ExcelProperty("需求名称")
    @ColumnWidth(35)
    private String demandName;

    @ExcelProperty("投产内容说明")
    private String prodContent;

    @ExcelProperty("关联特性")
    private String relaFeature;

    @ExcelProperty("关联脚本")
    private String relaScript;

    @ExcelProperty("产品类型")
    private String prodType;

    @ExcelProperty("需求经理")
    private String demandManager;

    @ExcelProperty("技术经理")
    private String techManager;

    @ExcelProperty("UAT环境检查")
    private String uatEnvCheck;

    @ExcelProperty("生产环境检查")
    private String prodEnvCheck;

    @ExcelProperty("备注事项")
    private String remark;

    /** 附件相对路径，多个以逗号分隔 */
    @ExcelIgnore
    private String attachmentPath;

    @ExcelProperty("创建人")
    private String createUser;

    @TableField(fill = FieldFill.INSERT)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @ExcelIgnore
    private String updateUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @ExcelIgnore
    private LocalDateTime updateTime;

    /** 所属部门 ID */
    private Long deptId;

    /** 部门名称（仅展示，非表字段） */
    @TableField(exist = false)
    private String deptName;
}
