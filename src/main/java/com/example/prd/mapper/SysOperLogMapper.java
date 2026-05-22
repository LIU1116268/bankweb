package com.example.prd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.prd.entity.SysOperLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统操作审计日志 - 数据访问层
 */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {
}
