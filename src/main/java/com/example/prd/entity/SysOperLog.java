package com.example.prd.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("sys_oper_log")
public class SysOperLog {


    @ExcelProperty("日志编号")
    @TableId(type = IdType.AUTO)
    private Long id;

    private String method;
    private String jsonResult;
    private String errorMsg;

    @ExcelProperty("系统模块")
    private String title;

    @ExcelProperty("操作类型")
    private String businessType;

    @ExcelProperty("操作人员")
    private String operUser;

    @ExcelProperty("请求地址")
    private String operUrl;

    @ExcelProperty("操作状态")// 0表示成功
    private Integer status;

    @ExcelProperty("操作时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date operTime;

    @ExcelIgnore
    private String operParam;
}