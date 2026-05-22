package com.example.prd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.prd.entity.SysDept;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 组织架构（部门）- 数据访问层
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 条件查询部门列表（默认只查 status=0 正常部门）
     */
    @SuppressWarnings("SqlResolve")
    @Select("<script>" +
            "SELECT dept_id, parent_id, dept_name, order_num, status " +
            "FROM sys_dept " +
            "<where>" +
            "  <if test='deptName != null and deptName != \"\"'>" +
            "    AND dept_name LIKE CONCAT('%', #{deptName}, '%')" +
            "  </if>" +
            "  AND status = '0'" +
            "</where>" +
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

    @Select("SELECT dept_id, parent_id, dept_name, order_num, status FROM sys_dept WHERE dept_id = #{deptId}")
    @ResultMap("SysDeptMap")
    SysDept selectDeptById(Long deptId);
}
