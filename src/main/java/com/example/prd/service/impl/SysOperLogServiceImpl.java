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
     */
    @Async
    @Override
    public void insertOperLog(SysOperLog operLog) {
        operLogMapper.insert(operLog);
    }

    @Override
    public List<SysOperLog> selectLogList() {
        // 使用 MyBatis-Plus 查询最近的 50 条日志，按时间倒序排列
        QueryWrapper<SysOperLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("OPER_TIME").last("LIMIT 50");
        return operLogMapper.selectList(queryWrapper);
    }

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
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("系统审计日志_" + System.currentTimeMillis(), "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        QueryWrapper<SysOperLog> queryWrapper = new QueryWrapper<>();
        // 强制：如果前端没传时间，默认只查最近 7 天，防止数据量过大
        if (beginTime != null && endTime != null) {
            queryWrapper.between("OPER_TIME", beginTime, endTime);
        } else {
            queryWrapper.last("LIMIT 1000"); // 兜底：最多只给导 1000 条
        }
        queryWrapper.orderByDesc("OPER_TIME");

        List<SysOperLog> list = operLogMapper.selectList(queryWrapper);
        EasyExcel.write(response.getOutputStream(), SysOperLog.class).sheet("审计日志").doWrite(list);
    }


}
