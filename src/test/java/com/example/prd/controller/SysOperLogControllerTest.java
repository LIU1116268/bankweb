package com.example.prd.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc // 自动配置 MockMvc 模拟请求
public class SysOperLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testListLogs() throws Exception {
        mockMvc.perform(get("/sysLog/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // 期待返回 200 状态码
                .andExpect(jsonPath("$.code").value(200)); // 期待 Result 里的 code 是 200
    }

    @Test
    public void testExportLog() throws Exception {
        mockMvc.perform(get("/sysLog/export"))
                .andExpect(status().isOk()); // 验证导出接口是否能正常响应
    }
}