package com.example.prd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.prd.entity.SysDept;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 查询部门列表
     * 修复提示：如果 IDE 报红，通常是因为它不认识 <script>。
     * 实际上代码运行是没问题的，通过在方法上添加 @SuppressWarnings("SqlResolve") 来压制 IDE 报错。
     */
    @SuppressWarnings("SqlResolve")
    @Select("<script>" +
            "SELECT dept_id, parent_id, dept_name, order_num, status " +
            "FROM sys_dept " +
            "<where> " +
            "  <if test='deptName != null and deptName != \"\"'> " +
            "    AND dept_name LIKE CONCAT('%', #{deptName}, '%') " +
            "  </if> " +
            "  AND status = '0' " +
            "</where> " +
            "ORDER BY parent_id, order_num" +
            "</script>")
    @Results(id = "SysDeptMap", value = {
            @Result(column = "dept_id", property = "deptId", id = true),
            @Result(column = "parent_id", property = "parentId"),
            @Result(column = "dept_name", property = "deptName"),
            @Result(column = "order_num", property = "orderNum"),
            @Result(column = "status", property = "status")
    })
    List<SysDept> selectDeptList(SysDept dept);

    /**
     * 根据 ID 查询单个部门详情
     * 将 FROM 后的 dept_id 改回 sys_dept
     */
    @Select("SELECT dept_id, parent_id, dept_name, order_num, status FROM sys_dept WHERE dept_id = #{deptId}")
    @ResultMap("SysDeptMap")
    SysDept selectDeptById(Long deptId);
}