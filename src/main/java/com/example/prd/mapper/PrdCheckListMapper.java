package com.example.prd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.prd.entity.PrdCheckList;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 产品核对清单 Mapper 接口
 * 继承 BaseMapper 以获得 MyBatis-Plus 的基础 CRUD 能力
 */

@Mapper
public interface PrdCheckListMapper extends BaseMapper<PrdCheckList> {

    /**
     * 动态插入记录（Selective）：仅插入对象中非空的字段。
     * 1. 使用 <script> 以支持动态 SQL 标签。
     * 2. <trim> 标签用于自动处理 SQL 括号，并移除末尾多余的逗号（suffixOverrides）。
     * 3. CREATE_TIME 默认调用数据库 NOW() 函数。
     */
    @Insert("<script>" +
            "INSERT INTO prd_check_list " +
            "<trim prefix='(' suffix=')' suffixOverrides=','> " +
            // 先判断是否为空
            "  <if test='id != null'>ID,</if> " +
            "  <if test='windowVerId != null'>WINDOW_VER_ID,</if> " +
            "  <if test='demandName != null'>DEMAND_NAME,</if> " +
            "  <if test='prodContent != null'>PROD_CONTENT,</if> " +
            "  <if test='relaFeature != null'>RELA_FEATURE,</if> " +
            "  <if test='relaScript != null'>RELA_SCRIPT,</if> " +
            "  <if test='prodType != null'>PROD_TYPE,</if> " +
            "  <if test='demandManager != null'>DEMAND_MANAGER,</if> " +
            "  <if test='techManager != null'>TECH_MANAGER,</if> " +
            "  <if test='uatEnvCheck != null'>UAT_ENV_CHECK,</if> " +
            "  <if test='prodEnvCheck != null'>PROD_ENV_CHECK,</if> " +
            "  <if test='remark != null'>REMARK,</if> " +
            "  <if test='attachmentPath != null'>ATTACHMENT_PATH,</if> " +
            "  <if test='createUser != null'>CREATE_USER,</if> " +
            "  CREATE_TIME " +
            "</trim> " +
            // 传入值
            "<trim prefix='VALUES (' suffix=')' suffixOverrides=','> " +
            "  <if test='id != null'>#{id},</if> " +
            "  <if test='windowVerId != null'>#{windowVerId},</if> " +
            "  <if test='demandName != null'>#{demandName},</if> " +
            "  <if test='prodContent != null'>#{prodContent},</if> " +
            "  <if test='relaFeature != null'>#{relaFeature},</if> " +
            "  <if test='relaScript != null'>#{relaScript},</if> " +
            "  <if test='prodType != null'>#{prodType},</if> " +
            "  <if test='demandManager != null'>#{demandManager},</if> " +
            "  <if test='techManager != null'>#{techManager},</if> " +
            "  <if test='uatEnvCheck != null'>#{uatEnvCheck},</if> " +
            "  <if test='prodEnvCheck != null'>#{prodEnvCheck},</if> " +
            "  <if test='remark != null'>#{remark},</if> " +
            "  <if test='attachmentPath != null'>#{attachmentPath},</if> " +
            "  <if test='createUser != null'>#{createUser},</if> " +
            "  NOW() " +
            "</trim> " +
            "</script>")
    int insertSelective(PrdCheckList record);

    /**
     * 根据主键动态更新（Selective）：仅更新非空属性，保持其他字段原样。
     * <set> 标签会自动处理 SQL 的 SET 关键字，并智能删除末尾多余的逗号。
     */
    @Update("<script> " +
            "UPDATE prd_check_list " +
            "<set> " +
            "  <if test='windowVerId != null'>WINDOW_VER_ID = #{windowVerId},</if> " +
            "  <if test='demandName != null'>DEMAND_NAME = #{demandName},</if> " +
            "  <if test='prodContent != null'>PROD_CONTENT = #{prodContent},</if> " +
            "  <if test='relaFeature != null'>RELA_FEATURE = #{relaFeature},</if> " +
            "  <if test='relaScript != null'>RELA_SCRIPT = #{relaScript},</if> " +
            "  <if test='prodType != null'>PROD_TYPE = #{prodType},</if> " +
            "  <if test='demandManager != null'>DEMAND_MANAGER = #{demandManager},</if> " +
            "  <if test='techManager != null'>TECH_MANAGER = #{techManager},</if> " +
            "  <if test='uatEnvCheck != null'>UAT_ENV_CHECK = #{uatEnvCheck},</if> " +
            "  <if test='prodEnvCheck != null'>PROD_ENV_CHECK = #{prodEnvCheck},</if> " +
            "  <if test='remark != null'>REMARK = #{remark},</if> " +
            "  <if test='attachmentPath != null'>ATTACHMENT_PATH = #{attachmentPath},</if> " +
            "  <if test='updateUser != null'>UPDATE_USER = #{updateUser},</if> " +
            "  UPDATE_TIME = NOW() " +
            "</set> " +
            "WHERE ID = #{id} " +
            "</script>")
    int updateByPrimaryKeySelective(PrdCheckList record);

    /**
     * 根据主键查询
     * @Results 定义结果集映射，解决数据库下划线字段(SNAKE_CASE)与Java驼峰属性(camelCase)的对应关系。
     * 这里的 id="PrdMap" 可供后续查询方法通过 @ResultMap("PrdMap") 复用。
     */
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
     * 分页条件查询
     * 1. <where> 标签：如果内部条件都不成立则不生成 WHERE，否则自动处理第一个 AND。
     * 2. LIKE CONCAT：数据库无关的模糊查询拼接方式。
     * 3. LIMIT：手动分页实现，offset 为跳过的记录数，limit 为查询条数。
     */
    @Select(
            // 动态sql说明
            "<script> " +
            // 1. 基础SQL：从 prd_check_list 表里查所有字段
            "SELECT * FROM prd_check_list " +
            // 2. 动态条件标签：自动帮我处理 WHERE 和多余的 AND，不会报错
            "<where> " +

            // ======================================
            // 条件1：如果 需求名称 不为空，就按名称模糊查询
            // ======================================
            "  <if test='demandName != null and demandName != \"\"'> " +
            "    AND DEMAND_NAME LIKE CONCAT('%', #{demandName}, '%') " +
            "  </if> " +
            // ======================================
            // 条件2：如果 部门ID列表 不为空，就按部门 IN 查询
            // ======================================
            "  <if test='deptIds != null and deptIds.size() > 0'> " +
            "    AND DEPT_ID IN " +

            // 循环标签：把 List<Long> 变成 (1,2,3) 这种格式
            "<foreach " +
            "collection='deptIds' " +   // 要循环的集合
            "item='id' " +               // 每次循环的变量名
            "open='(' " +                // 开头加 (
            "separator=',' " +           // 中间加 ,
            "close=')'> " +             // 结尾加 )
            "      #{id} " +            // 填入每个id
            "</foreach> " +

            "  </if> " +

            "</where> " +
            // 3. 排序：按创建时间 最新的排在前面
            "ORDER BY CREATE_TIME DESC " +
            // 4. 分页：offset = 从第几条开始查；limit = 查多少条
            "LIMIT #{offset}, #{limit} " +

            "</script>")
    @ResultMap("PrdMap")
    List<PrdCheckList> selectByCondition(@Param("demandName") String demandName,
                                         @Param("deptIds") List<Long> deptIds,
                                         @Param("offset") long offset,
                                         @Param("limit") int limit);

    /**
     * 全量条件查询（主要用于数据导出）
     * 不带 LIMIT 限制，按需求名称模糊过滤。
     */
    @Select("<script> " +
            "SELECT * FROM prd_check_list " +
            "<where> " +
            "  <if test='demandName != null and demandName != \"\"'> " +
            "    AND DEMAND_NAME LIKE CONCAT('%', #{demandName}, '%') " +
            "  </if> " +
            "</where> " +
            "ORDER BY CREATE_TIME DESC " +
            "</script>")
    @ResultMap("PrdMap")
    List<PrdCheckList> selectAll(@Param("demandName") String demandName);




}