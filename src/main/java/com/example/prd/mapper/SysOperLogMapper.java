package com.example.prd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper; // 必须导入这个
import com.example.prd.entity.SysOperLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {
    // 确保继承了 BaseMapper，这样 selectList 等方法才会自动注入
}