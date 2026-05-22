package com.example.prd.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.prd.entity.SysOperLog;
import com.example.prd.mapper.SysOperLogMapper;
import com.example.prd.service.SysOperLogService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 系统操作审计日志 - 业务实现
 */
@Service
public class SysOperLogServiceImpl implements SysOperLogService {

    private static final int LIST_LIMIT = 50;
    private static final int EXPORT_DEFAULT_LIMIT = 1000;

    @Autowired
    private SysOperLogMapper operLogMapper;

    @Override
    @Async
    public void insertOperLog(SysOperLog operLog) {
        operLogMapper.insert(operLog);
    }

    @Override
    public List<SysOperLog> selectLogList() {
        QueryWrapper<SysOperLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("OPER_TIME").last("LIMIT " + LIST_LIMIT);
        return operLogMapper.selectList(queryWrapper);
    }

    @Override
    public void exportLog(HttpServletResponse response) throws IOException {
        setExcelResponseHeader(response, "系统操作日志_");
        List<SysOperLog> list = operLogMapper.selectList(null);
        EasyExcel.write(response.getOutputStream(), SysOperLog.class)
                .sheet("操作日志")
                .doWrite(list);
    }

    @Override
    public void exportLog(String beginTime, String endTime, HttpServletResponse response) throws IOException {
        setExcelResponseHeader(response, "系统审计日志_");

        QueryWrapper<SysOperLog> queryWrapper = new QueryWrapper<>();
        if (beginTime != null && endTime != null) {
            queryWrapper.between("OPER_TIME", beginTime, endTime);
        } else {
            queryWrapper.last("LIMIT " + EXPORT_DEFAULT_LIMIT);
        }
        queryWrapper.orderByDesc("OPER_TIME");

        List<SysOperLog> list = operLogMapper.selectList(queryWrapper);
        EasyExcel.write(response.getOutputStream(), SysOperLog.class)
                .sheet("审计日志")
                .doWrite(list);
    }

    private void setExcelResponseHeader(HttpServletResponse response, String namePrefix) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(namePrefix + System.currentTimeMillis(), StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
    }
}
