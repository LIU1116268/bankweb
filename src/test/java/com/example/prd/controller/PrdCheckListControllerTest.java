package com.example.prd.controller;

import com.alibaba.fastjson2.JSON;
import com.example.prd.entity.PrdCheckList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PrdCheckListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试分页列表
     */
    @Test
    public void testPageList() throws Exception {
        mockMvc.perform(get("/prd/list")
                        .param("current", "1")
                        .param("size", "5")
                        .param("demandName", "信用卡"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试新增保存 (JSON 交互)
     */
    @Test
    public void testSavePrd() throws Exception {
        PrdCheckList prd = new PrdCheckList();
        prd.setDemandName("单元测试需求");
        prd.setWindowVerId("1");

        mockMvc.perform(post("/prd/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(prd))) // 将对象转为 JSON 字符串
                .andExpect(status().isOk());
    }

    /**
     * 测试文件上传 (MockMultipartFile)
     */
    @Test
    public void testUpload() throws Exception {
        // 模拟一个文件
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.sql",
                MediaType.TEXT_PLAIN_VALUE,
                "CREATE TABLE test...".getBytes()
        );

        mockMvc.perform(multipart("/prd/upload").file(file))
                .andExpect(status().isOk());
    }

    @Test
    public void testExportExcel() throws Exception {
        mockMvc.perform(get("/prd/exportExcel")
                        .param("demandName", "信用卡"))
                .andExpect(status().isOk())
                // 验证 Header 是否包含 Excel 的 MIME 类型
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8"))
                // 验证是否触发了下载响应头
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    public void testExportZip() throws Exception {
        mockMvc.perform(get("/prd/exportZip")
                        .param("ids", "uuid-1,uuid-2")) // 使用 GET 参数传递，逗号分隔
                .andExpect(status().isOk());
    }


    @Test
    public void testUploadAndBind() throws Exception {
        // 1. 先准备一个文件
        MockMultipartFile file = new MockMultipartFile("files", "test.pdf", "application/pdf", "hello".getBytes());

        // 2. 模拟一个存在的 ID (你可以直接用之前保存测试里的逻辑，或者写个真实的 ID)
        String realId = "PCL2026031800000004";

        mockMvc.perform(multipart("/prd/uploadBind")
                        .file(file)
                        .param("id", realId))
                .andExpect(status().isOk());

    }
}