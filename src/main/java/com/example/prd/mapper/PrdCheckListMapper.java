package com.example.prd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.prd.entity.PrdCheckList;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * PRD 投产检查清单 - 数据访问层
 * <p>
 * 继承 BaseMapper 获得通用 CRUD；复杂 SQL 使用注解动态拼接
 */
@Mapper
public interface PrdCheckListMapper extends BaseMapper<PrdCheckList> {

    /**
     * 动态插入：仅插入非空字段
     */
    @Insert("<script>" +
            "INSERT INTO prd_check_list " +
            "<trim prefix='(' suffix=')' suffixOverrides=',' >" +
            "  <if test='id != null'>ID,</if>" +
            "  <if test='windowVerId != null'>WINDOW_VER_ID,</if>" +
            "  <if test='demandName != null'>DEMAND_NAME,</if>" +
            "  <if test='prodContent != null'>PROD_CONTENT,</if>" +
            "  <if test='relaFeature != null'>RELA_FEATURE,</if>" +
            "  <if test='relaScript != null'>RELA_SCRIPT,</if>" +
            "  <if test='prodType != null'>PROD_TYPE,</if>" +
            "  <if test='demandManager != null'>DEMAND_MANAGER,</if>" +
            "  <if test='techManager != null'>TECH_MANAGER,</if>" +
            "  <if test='uatEnvCheck != null'>UAT_ENV_CHECK,</if>" +
            "  <if test='prodEnvCheck != null'>PROD_ENV_CHECK,</if>" +
            "  <if test='remark != null'>REMARK,</if>" +
            "  <if test='attachmentPath != null'>ATTACHMENT_PATH,</if>" +
            "  <if test='createUser != null'>CREATE_USER,</if>" +
            "  CREATE_TIME" +
            "</trim>" +
            "<trim prefix='VALUES (' suffix=')' suffixOverrides=','>" +
            "  <if test='id != null'>#{id},</if>" +
            "  <if test='windowVerId != null'>#{windowVerId},</if>" +
            "  <if test='demandName != null'>#{demandName},</if>" +
            "  <if test='prodContent != null'>#{prodContent},</if>" +
            "  <if test='relaFeature != null'>#{relaFeature},</if>" +
            "  <if test='relaScript != null'>#{relaScript},</if>" +
            "  <if test='prodType != null'>#{prodType},</if>" +
            "  <if test='demandManager != null'>#{demandManager},</if>" +
            "  <if test='techManager != null'>#{techManager},</if>" +
            "  <if test='uatEnvCheck != null'>#{uatEnvCheck},</if>" +
            "  <if test='prodEnvCheck != null'>#{prodEnvCheck},</if>" +
            "  <if test='remark != null'>#{remark},</if>" +
            "  <if test='attachmentPath != null'>#{attachmentPath},</if>" +
            "  <if test='createUser != null'>#{createUser},</if>" +
            "  NOW()" +
            "</trim>" +
            "</script>")
    int insertSelective(PrdCheckList record);

    /**
     * 动态更新：仅更新非空字段
     */
    @Update("<script>" +
            "UPDATE prd_check_list " +
            "<set>" +
            "  <if test='windowVerId != null'>WINDOW_VER_ID = #{windowVerId},</if>" +
            "  <if test='demandName != null'>DEMAND_NAME = #{demandName},</if>" +
            "  <if test='prodContent != null'>PROD_CONTENT = #{prodContent},</if>" +
            "  <if test='relaFeature != null'>RELA_FEATURE = #{relaFeature},</if>" +
            "  <if test='relaScript != null'>RELA_SCRIPT = #{relaScript},</if>" +
            "  <if test='prodType != null'>PROD_TYPE = #{prodType},</if>" +
            "  <if test='demandManager != null'>DEMAND_MANAGER = #{demandManager},</if>" +
            "  <if test='techManager != null'>TECH_MANAGER = #{techManager},</if>" +
            "  <if test='uatEnvCheck != null'>UAT_ENV_CHECK = #{uatEnvCheck},</if>" +
            "  <if test='prodEnvCheck != null'>PROD_ENV_CHECK = #{prodEnvCheck},</if>" +
            "  <if test='remark != null'>REMARK = #{remark},</if>" +
            "  <if test='attachmentPath != null'>ATTACHMENT_PATH = #{attachmentPath},</if>" +
            "  <if test='updateUser != null'>UPDATE_USER = #{updateUser},</if>" +
            "  UPDATE_TIME = NOW()" +
            "</set>" +
            "WHERE ID = #{id}" +
            "</script>")
    int updateByPrimaryKeySelective(PrdCheckList record);

    @Select("SELECT * FROM prd_check_list WHERE ID = #{id}")
    @Results(id = "PrdMap", value = {
            @Result(column = "ID", property = "id", id = true),
            @Result(column = "WINDOW_VER_ID", property = "windowVerId"),
            @Result(column = "DEMAND_NAME", property = "demandName"),
            @Result(column = "PROD_CONTENT", property = "prodContent"),
            @Result(column = "RELA_FEATURE", property = "relaFeature"),
            @Result(column = "RELA_SCRIPT", property = "relaScript"),
            @Result(column = "PROD_TYPE", property = "prodType"),
            @Result(column = "DEMAND_MANAGER", property = "demandManager"),
            @Result(column = "TECH_MANAGER", property = "techManager"),
            @Result(column = "UAT_ENV_CHECK", property = "uatEnvCheck"),
            @Result(column = "PROD_ENV_CHECK", property = "prodEnvCheck"),
            @Result(column = "REMARK", property = "remark"),
            @Result(column = "ATTACHMENT_PATH", property = "attachmentPath"),
            @Result(column = "CREATE_USER", property = "createUser"),
            @Result(column = "CREATE_TIME", property = "createTime"),
            @Result(column = "UPDATE_USER", property = "updateUser"),
            @Result(column = "UPDATE_TIME", property = "updateTime")
    })
    PrdCheckList selectByPrimaryKey(String id);

    /**
     * 分页条件查询（支持需求名模糊、部门 IN 过滤）
     */
    @Select("<script>" +
            "SELECT * FROM prd_check_list " +
            "<where>" +
            "  <if test='demandName != null and demandName != \"\"'>" +
            "    AND DEMAND_NAME LIKE CONCAT('%', #{demandName}, '%')" +
            "  </if>" +
            "  <if test='deptIds != null and deptIds.size() > 0'>" +
            "    AND DEPT_ID IN " +
            "    <foreach collection='deptIds' item='id' open='(' separator=',' close=')'>" +
            "      #{id}" +
            "    </foreach>" +
            "  </if>" +
            "</where>" +
            "ORDER BY CREATE_TIME DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    @ResultMap("PrdMap")
    List<PrdCheckList> selectByCondition(
            @Param("demandName") String demandName,
            @Param("deptIds") List<Long> deptIds,
            @Param("offset") long offset,
            @Param("limit") int limit);

    /**
     * 全量条件查询（用于 Excel 导出，无 LIMIT）
     */
    @Select("<script>" +
            "SELECT * FROM prd_check_list " +
            "<where>" +
            "  <if test='demandName != null and demandName != \"\"'>" +
            "    AND DEMAND_NAME LIKE CONCAT('%', #{demandName}, '%')" +
            "  </if>" +
            "</where>" +
            "ORDER BY CREATE_TIME DESC" +
            "</script>")
    @ResultMap("PrdMap")
    List<PrdCheckList> selectAll(@Param("demandName") String demandName);
}
