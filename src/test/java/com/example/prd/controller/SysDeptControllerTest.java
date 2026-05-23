package com.example.prd.controller;

import com.alibaba.fastjson2.JSON;
import com.example.prd.entity.SysDept;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SysDeptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试获取部门树
     * <p>
     * 请求：GET /system/dept/tree
     */
    @Test
    public void testTree() throws Exception {
        mockMvc.perform(get("/system/dept/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试获取部门树（带查询参数）
     * <p>
     * 请求：GET /system/dept/tree?deptName=支行
     */
    @Test
    public void testTreeWithFilter() throws Exception {
        mockMvc.perform(get("/system/dept/tree")
                        .param("deptName", "支行"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试新增部门
     * <p>
     * 请求：POST /system/dept/add
     * Body 示例：
     * {
     *   "deptName": "科华支行",
     *   "parentId": 105,
     *   "orderNum": 1
     * }
     */
    @Test
    public void testAddDept() throws Exception {
        SysDept dept = new SysDept();
        dept.setDeptName("测试支行");
        dept.setParentId(100L);
        dept.setOrderNum(1);
        dept.setStatus("0");

        mockMvc.perform(post("/system/dept/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(dept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试新增部门（缺少必要字段）
     * <p>
     * 验证服务层对空值的处理
     */
    @Test
    public void testAddDeptWithoutName() throws Exception {
        SysDept dept = new SysDept();
        dept.setParentId(100L);
        dept.setOrderNum(1);

        mockMvc.perform(post("/system/dept/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(dept)))
                .andExpect(status().isOk());
    }

    /**
     * 测试新增部门（parentId为null）
     * <p>
     * 验证顶级部门的创建
     */
    @Test
    public void testAddTopLevelDept() throws Exception {
        SysDept dept = new SysDept();
        dept.setDeptName("测试总行");
        dept.setOrderNum(0);
        dept.setStatus("0");

        mockMvc.perform(post("/system/dept/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(dept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
