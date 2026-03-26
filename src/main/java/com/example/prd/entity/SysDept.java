package com.example.prd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("sys_dept")
public class SysDept {
    @TableId(type = IdType.AUTO)
    private Long deptId;      // 部门ID
    private Long parentId;    // 父部门ID
    private String deptName;  // 部门名称
    private Integer orderNum; // 显示顺序
    private String status;    // 状态（0正常 1停用）

    // 非数据库字段，用于存放子节点
    // 数据库表 sys_dept 里并没有 children 这一列。但在 Java 对象里，需要一个地方来存放“我的下属部门列表
    @TableField(exist = false)
    private List<SysDept> children = new ArrayList<>();
}