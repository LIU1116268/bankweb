package com.example.prd.controller;

import com.example.prd.common.Result;
import com.example.prd.entity.SysDept;
import com.example.prd.service.SysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/dept")
public class SysDeptController {
    @Autowired
    private SysDeptService deptService;

    /**
     * 获取所有部门树形汇总列表，方便preCheckList页面选择部门,
     * URL: http://localhost:8080/system/dept/tree
     */
    @GetMapping("/tree")
    public Result list(SysDept dept) {
        List<SysDept> depts = deptService.selectDeptList(dept);
        return Result.success(deptService.buildDeptTree(depts));
    }


    /**
     * 保存部门信息并更新缓存
     * 1. 数据持久化到 MySQL 数据库，保证数据安全可靠
     * 2. 删除 Redis 缓存，而非直接更新缓存
     *
     * 此处采用【删除缓存】而非【更新缓存】，原因如下：
     * 1. 部门树是递归构建的复杂结构，若直接在缓存中更新节点，逻辑复杂、易出错
     * 2. 删除缓存后，采用延迟加载（懒加载）策略：
     *    下次查询时若缓存不存在，则从数据库查询最新全量数据，重新构建并回填缓存
     * 3. 该方案实现简单、稳定性高，能保证缓存与数据库数据强一致
     */
    @PostMapping("/add")
    public Result add(@RequestBody SysDept dept) {
        Result result;
        int rows = deptService.insertDept(dept);
        if (rows > 0) {
            deptService.clearDeptCache();
            result = Result.success();
        } else {
            result = Result.error("新增失败");
        }
        return result;
    }
}
