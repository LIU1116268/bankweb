package com.example.prd.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 系统操作审计日志 - 实体
 * <p>
 * 对应表：sys_oper_log；由 {@link com.example.prd.aspect.LogAspect} 自动写入
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog {

    @TableId(type = IdType.AUTO)
    @ExcelProperty("日志编号")
    private Long id;

    @ExcelProperty("系统模块")
    private String title;

    @ExcelProperty("操作类型")
    private String businessType;

    @ExcelProperty("操作人员")
    private String operUser;

    @ExcelProperty("请求地址")
    private String operUrl;

    /** 0 成功，1 失败 */
    @ExcelProperty("操作状态")
    private Integer status;

    @ExcelProperty("操作时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date operTime;

    private String method;

    @ExcelIgnore
    private String operParam;

    @ExcelIgnore
    private String jsonResult;

    @ExcelIgnore
    private String errorMsg;
}
