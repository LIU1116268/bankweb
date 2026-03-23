package com.example.prd.service;

import com.example.prd.entity.SysDept;
import java.util.List;

public interface SysDeptService {
    // 获取部门列表
    List<SysDept> selectDeptList(SysDept dept);

    // 构建树形结构（带缓存）
    List<SysDept> buildDeptTree(List<SysDept> depts);

    // 根据 ID 获取所有子部门 ID（带缓存）
    List<Long> selectChildrenIds(Long deptId);

    int insertDept(SysDept dept);

    // 清理缓存（供新增/修改/删除时调用）
    void clearDeptCache();
}