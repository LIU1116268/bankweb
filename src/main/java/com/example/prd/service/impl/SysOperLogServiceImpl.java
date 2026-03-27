package com.example.prd.service.impl;

import com.alibaba.excel.EasyExcel;
import com.example.prd.entity.SysOperLog;
import com.example.prd.mapper.SysOperLogMapper;
import com.example.prd.service.SysOperLogService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

@Service
public class SysOperLogServiceImpl implements SysOperLogService {

    @Autowired
    private SysOperLogMapper operLogMapper;

    /**
     * @Async 注解标记这是一个异步方法。
     * 只要在主启动类加上 @EnableAsync，Spring 就会在独立的线程池中运行此方法，
     * 不会占用主业务请求的响应时间
     * 切面时使用，插入Controller中操作记录
     */
    @Async
    @Override
    public void insertOperLog(SysOperLog operLog) {
        operLogMapper.insert(operLog);
    }

    @Override
    public List<SysOperLog> selectLogList() {// 返回一组日志数据
        // 使用 MyBatis-Plus 查询最近的 50 条日志，按时间倒序排列

        // 1. 创建 QueryWrapper 查询条件构造器（用于拼接 SQL 查询条件）
        QueryWrapper<SysOperLog> queryWrapper = new QueryWrapper<>();
        // 2. 拼接排序 + 限制条数：按操作时间倒序排序，只查询 50 条数据
        queryWrapper.orderByDesc("OPER_TIME").last("LIMIT 50");
        // 3. 传入拼接好的查询条件，调用 BaseMapper 的 selectList 方法执行查询
        return operLogMapper.selectList(queryWrapper);
    }

    // 全部导出没有限制
    @Override
    public void exportLog(HttpServletResponse response) throws IOException {
        // 1. 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("系统操作日志_" + System.currentTimeMillis(), "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 2. 查询所有日志
        List<SysOperLog> list = operLogMapper.selectList(null);

        // 3. 写入 Excel
        EasyExcel.write(response.getOutputStream(), SysOperLog.class)
                .sheet("操作日志")
                .doWrite(list);
    }

    @Override
    public void exportLog(String beginTime, String endTime, HttpServletResponse response) throws IOException {
        // 1. 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("系统审计日志_" + System.currentTimeMillis(), "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        QueryWrapper<SysOperLog> queryWrapper = new QueryWrapper<>();
        if (beginTime != null && endTime != null) {
            // 拼接查询条件：按操作时间范围查询
            queryWrapper.between("OPER_TIME", beginTime, endTime);
        } else {
            // 没有时间限制就兜底：最多只给导 1000 条
            queryWrapper.last("LIMIT 1000");
        }
        // 按操作时间倒序排序
        queryWrapper.orderByDesc("OPER_TIME");

        List<SysOperLog> list = operLogMapper.selectList(queryWrapper);
        EasyExcel.write(response.getOutputStream(), SysOperLog.class).sheet("审计日志").doWrite(list);
    }


}
