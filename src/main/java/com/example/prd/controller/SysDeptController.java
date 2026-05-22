package com.example.prd.controller;

import com.example.prd.common.Result;
import com.example.prd.entity.SysDept;
import com.example.prd.service.SysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织架构（部门）- 接口层
 * <p>
 * 模块职责：部门树查询、部门新增；配合 Redis 缓存与变更时缓存失效
 * <p>
 * 基础地址：{@code http://localhost:8080/system/dept}
 */
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {

    @Autowired
    private SysDeptService deptService;

    /**
     * 获取部门树（供前端下拉/树形选择）
     * <p>
     * 请求：GET /system/dept/tree
     * <p>
     * 说明：先查库得到扁平列表，再由 Service 递归组装为树；结果带 Redis 缓存
     * <p>
     * 测试用例：
     * <pre>
     * GET http://localhost:8080/system/dept/tree
     * </pre>
     */
    @GetMapping("/tree")
    public Result<List<SysDept>> tree(SysDept dept) {
        List<SysDept> depts = deptService.selectDeptList(dept);
        return Result.success(deptService.buildDeptTree(depts));
    }

    /**
     * 新增部门
     * <p>
     * 请求：POST /system/dept/add<br>
     * Content-Type：application/json
     * <p>
     * 缓存策略：新增成功后删除 Redis 中的部门树/子部门 ID 缓存（非直接改缓存），
     * 下次查询时从数据库重建，保证数据一致且实现简单。
     * <p>
     * 测试用例：
     * <pre>
     * POST http://localhost:8080/system/dept/add
     * Body 示例：
     * {
     *   "deptName": "科华支行",
     *   "parentId": 105,
     *   "orderNum": 1
     * }
     * </pre>
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody SysDept dept) {
        int rows = deptService.insertDept(dept);
        if (rows > 0) {
            deptService.clearDeptCache();
            return Result.success();
        }
        return Result.error("新增失败");
    }
}
