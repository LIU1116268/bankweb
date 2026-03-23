package com.example.prd.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("prd_check_list")
@HeadRowHeight(25)     // 设置表头高度
@ContentRowHeight(20)  // 设置内容行高
@ColumnWidth(20)       // 设置默认列宽
public class PrdCheckList {

    @TableId(type = IdType.ASSIGN_ID)
    @ExcelProperty("数据ID") // 如果不想导出 ID，可以换成 @ExcelIgnore
    private String id;

    @ExcelProperty("投产窗口版本")
    private String windowVerId;

    @ExcelProperty("需求名称")
    @ColumnWidth(35) // 需求名称通常较长，单独设置宽度
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

    @ExcelIgnore // 导出 Excel 时忽略这个字段，因为路径对业务员没意义
    private String attachmentPath;

    @ExcelProperty("创建人")
    private String createUser;

    @TableField(fill = FieldFill.INSERT)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @ExcelIgnore // 更新人通常内部使用，导出时忽略
    private String updateUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @ExcelIgnore // 更新时间通常不需要导出
    private LocalDateTime updateTime;

    // 在 PrdCheckList 类中添加
    private Long deptId;

    @TableField(exist = false)
    private String deptName; // 用于展示部门名称，数据库不存
}