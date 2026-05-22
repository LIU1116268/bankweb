package com.example.prd.service;

import com.example.prd.entity.SysDept;

import java.util.List;

/**
 * 组织架构（部门）- 业务接口
 */
public interface SysDeptService {

    /** 查询部门扁平列表 */
    List<SysDept> selectDeptList(SysDept dept);

    /** 将扁平列表组装为树（带 Redis 缓存 + 分布式锁） */
    List<SysDept> buildDeptTree(List<SysDept> depts);

    /** 查询某部门及其全部下级部门 ID（带缓存） */
    List<Long> selectChildrenIds(Long deptId);

    /** 新增部门 */
    int insertDept(SysDept dept);

    /** 清理部门相关 Redis 缓存 */
    void clearDeptCache();
}
