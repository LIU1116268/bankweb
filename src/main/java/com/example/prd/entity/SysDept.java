package com.example.prd.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 组织架构（部门）- 实体
 * <p>
 * 对应表：sys_dept
 */
@Data
@TableName("sys_dept")
public class SysDept {

    @TableId(type = IdType.AUTO)
    private Long deptId;

    private Long parentId;

    private String deptName;

    private Integer orderNum;

    /** 状态：0 正常，1 停用 */
    private String status;

    /** 子部门列表（树形展示用，非数据库字段） */
    @TableField(exist = false)
    private List<SysDept> children = new ArrayList<>();
}
