package com.example.prd.controller;

import com.example.prd.annotation.Log;
import com.example.prd.annotation.RateLimit;
import com.example.prd.common.Result;
import com.example.prd.dto.PrdStatusTransitionRequest;
import com.example.prd.entity.PrdCheckList;
import com.example.prd.service.ExportTaskService;
import com.example.prd.service.PrdCheckListService;
import com.example.prd.vo.ExportTaskVO;
import com.example.prd.vo.PrdStatusTransitionVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * PRD 投产检查清单 - 接口层
 * <p>
 * 模块职责：清单 CRUD、投产状态机流转、分页检索、附件管理、Excel/ZIP 导出
 * <p>
 * 基础地址：{@code http://localhost:8080/prd}
 */
@RestController
@RequestMapping("/prd")
public class PrdCheckListController {

    @Autowired
    private PrdCheckListService prdService;

    @Autowired
    private ExportTaskService exportTaskService;

    // ==================== 数据维护 ====================

    /**
     * 新增或更新核对清单
     * <p>
     * 请求：POST /prd/save<br>
     * Content-Type：application/json
     * <p>
     * 规则：id 为空则新增（服务端生成 UUID）；id 不为空则按主键选择性更新
     * <p>
     * 测试用例：
     * <pre>
     * POST http://localhost:8080/prd/save
     * Body 示例（新增）：
     * {
     *   "windowVerId": "202603251",
     *   "demandName": "信用卡-分期额度动态测算",
     *   "prodContent": "前端UI适配-H9",
     *   "relaFeature": "feature-card-limit-02",
     *   "relaScript": "不涉及",
     *   "prodType": "功能新增",
     *   "demandManager": "范小凡",
     *   "techManager": "黄小布",
     *   "uatEnvCheck": "已通过",
     *   "prodEnvCheck": "待核对",
     *   "remark": "",
     *   "createUser": "104356"
     * }
     * </pre>
     * <p>
     * 注意：新增会写入 status=DRAFT，要求表 prd_check_list 已存在 STATUS 列，
     * 请先执行 {@code sql/prd_check_list_add_status.sql}，否则会报 500。
     * <p>
     * 机构数据权限（Apifox Headers）：
     * <pre>
     * X-Dept-Id: 101    （模拟四川省分行用户，新增记录默认归属该机构）
     * X-User-Id: 104356 （可选，工号）
     * </pre>
     */
    @Log(title = "核对清单", businessType = "SAVE")
    @PostMapping("/save")
    public Result<Void> save(@RequestBody PrdCheckList prd) {
        return prdService.saveWithCheck(prd) ? Result.success() : Result.error("保存失败");
    }

    /**
     * 按主键查询详情
     * <p>
     * 请求：GET /prd/detail/{id}
     * <p>
     * 测试用例：
     * <pre>
     * GET http://localhost:8080/prd/detail/PCL2026031800000003
     * </pre>
     */
    @GetMapping("/detail/{id}")
    public Result<PrdCheckList> detail(@PathVariable String id) {
        return Result.success(prdService.getById(id));
    }

    // ==================== 投产状态机 ====================

    /**
     * 状态流转（状态机核心接口）
     * <p>
     * 请求：POST /prd/transition<br>
     * Content-Type：application/json
     * <p>
     * 合法流转路径：
     * <pre>
     * 草稿(DRAFT) → 已提交(SUBMITTED) → UAT通过(UAT_PASSED) → 待投产(PROD_READY) → 已归档(ARCHIVED)
     * 已提交(SUBMITTED) 可打回 → 草稿(DRAFT)
     * </pre>
     * <p>
     * 本接口标注 {@link Log}，操作会异步写入 sys_oper_log，businessType=TRANSITION，便于审计追溯。
     * <p>
     * 测试用例：
     * <pre>
     * POST http://localhost:8080/prd/transition
     * Body 示例（提交评审）：
     * {
     *   "id": "202603180001",
     *   "targetStatus": "SUBMITTED",
     *   "operator": "104356",
     *   "remark": "材料已齐，提交UAT复核"
     * }
     *
     * Body 示例（UAT 通过）：
     * {
     *   "id": "202603180001",
     *   "targetStatus": "UAT_PASSED",
     *   "operator": "reviewer01",
     *   "remark": "UAT环境验证通过"
     * }
     * </pre>
     */
    @Log(title = "PRD状态流转", businessType = "TRANSITION")
    @PostMapping("/transition")
    public Result<PrdStatusTransitionVO> transition(@RequestBody PrdStatusTransitionRequest request) {
        try {
            return Result.success(prdService.transitionStatus(request));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询某记录当前允许流转到的下一状态（供前端按钮/下拉渲染）
     * <p>
     * 请求：GET /prd/transition/allowed/{id}
     * <p>
     * 测试用例：
     * <pre>
     * GET http://localhost:8080/prd/transition/allowed/202603180001
     * </pre>
     */
    @GetMapping("/transition/allowed/{id}")
    public Result<PrdStatusTransitionVO> allowedTransitions(@PathVariable String id) {
        try {
            return Result.success(prdService.getAllowedTransitions(id));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 分页查询 ====================

    /**
     * 分页列表（支持需求名模糊搜索、按部门范围过滤）
     * <p>
     * 请求：GET /prd/list
     * <p>
     * 参数说明：
     * <ul>
     *   <li>current - 当前页，默认 1</li>
     *   <li>size - 每页条数，默认 5</li>
     *   <li>demandName - 需求名称关键词（可选）</li>
     *   <li>deptId - 部门 ID（可选）</li>
     *   <li>recursive - 是否包含下级部门，默认 true</li>
     * </ul>
     * <p>
     * 测试用例：
     * <pre>
     * 1. 基础分页：
     *    GET http://localhost:8080/prd/list?current=1&amp;size=5
     *
     * 2. 按需求名搜索：
     *    GET http://localhost:8080/prd/list?current=1&amp;size=5&amp;demandName=信用卡
     *
     * 3. 查某分行及全部下级支行（默认递归）：
     *    GET http://localhost:8080/prd/list?deptId=101
     *
     * 4. 只查该分行本级（不含下级）：
     *    GET http://localhost:8080/prd/list?deptId=101&amp;recursive=false
     * </pre>
     * <p>
     * 机构数据权限（Apifox 在 Headers 里加）：
     * <pre>
     * X-Dept-Id: 101   → 只能看四川省分行及下属支行数据
     * X-Dept-Id: 102   → 只能看广东省分行及下属数据（与 101 结果不同）
     * 不传 X-Dept-Id   → 不做机构过滤（看全量，便于本地调试）
     * </pre>
     * <p>
     * 越权测试：Header 填 101，URL 加 deptId=102 → 应返回 code=403
     * <p>
     * 限流：同一 IP 每分钟最多 60 次
     */
    @RateLimit(rate = 60, interval = 1, key = "list")   // Redisson：同一 IP 每分钟最多 60 次
    @GetMapping("/list")
    public Result<List<PrdCheckList>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String demandName,
            @RequestParam(required = false) Long deptId,
            @RequestParam(defaultValue = "true") boolean recursive) {
        return Result.success(prdService.selectCustomPage(current, size, demandName, deptId, recursive));
    }

    // ==================== 附件管理 ====================

    /**
     * 多文件上传（仅上传，不绑定业务记录）
     * <p>
     * 请求：POST /prd/upload<br>
     * Content-Type：multipart/form-data<br>
     * 参数名：files（File 类型，可多选）
     * <p>
     * 返回：相对路径字符串，多个文件以英文逗号分隔
     * <p>
     * 存储根目录见 application.yml 中 file.upload-path（默认 D:/prd_attachments/）
     * <p>
     * 测试用例（Apifox / Postman）：
     * <pre>
     * POST http://localhost:8080/prd/upload
     * form-data: files = 选择 .sql / .pdf / .zip 文件（可多选）
     * </pre>
     * <p>
     * 限流：同一 IP 每分钟最多 10 次
     */
    @RateLimit(rate = 10, interval = 1, key = "upload")  // Redisson：同一 IP 每分钟最多 10 次上传
    @Log(title = "文件上传", businessType = "UPLOAD")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("files") MultipartFile[] files) {
        try {
            return Result.success(prdService.uploadFiles(files));
        } catch (Exception e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传并绑定到指定清单记录
     * <p>
     * 请求：POST /prd/uploadBind<br>
     * Content-Type：multipart/form-data
     * <p>
     * 参数：files（文件数组）、id（业务主键）
     * <p>
     * 测试用例：
     * <pre>
     * POST http://localhost:8080/prd/uploadBind?id=PCL2026031800000004
     * form-data: files = 选择附件
     * </pre>
     * <p>
     * 限流：同一 IP 每分钟最多 10 次
     */
    @RateLimit(rate = 10, interval = 1, key = "uploadBind") // Redisson：绑定上传限流
    @Log(title = "关联附件上传", businessType = "UPLOADBIND")
    @PostMapping("/uploadBind")
    public Result<String> uploadBind(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("id") String id) {
        try {
            String finalPath = prdService.uploadAndBind(files, id);
            return Result.success("文件已成功关联至记录：" + id, finalPath);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("服务器内部错误：" + e.getMessage());
        }
    }

    /**
     * 删除指定记录的全部附件（物理文件 + 清空数据库路径字段）
     * <p>
     * 请求：DELETE /prd/deleteFile/{id}
     * <p>
     * 测试用例：
     * <pre>
     * DELETE http://localhost:8080/prd/deleteFile/PCL2026031800000004
     * </pre>
     */
    @Log(title = "删除附件", businessType = "DELETE")
    @DeleteMapping("/deleteFile/{id}")
    public Result<Void> deleteFile(@PathVariable String id) {
        return prdService.deleteAttachment(id)
                ? Result.success("附件已清理", null)
                : Result.error("清理失败");
    }

    // ==================== 导出下载 ====================

    /**
     * 批量打包附件为 ZIP 下载
     * <p>
     * 请求：GET /prd/exportZip?ids=id1,id2
     * <p>
     * 说明：浏览器直接访问将触发文件下载；响应体为二进制流，非 JSON
     * <p>
     * 测试用例：
     * <pre>
     * GET http://localhost:8080/prd/exportZip?ids=PCL2026031800000004,PCL2026031800000003
     * </pre>
     * <p>
     * 限流：同一 IP 每分钟最多 5 次；超限返回 JSON：{"code":429,"msg":"访问过于频繁，请稍后再试"}
     */
    @RateLimit(rate = 5, interval = 1, key = "exportZip")   // Redisson：ZIP 导出限流，防打满磁盘/带宽
    @GetMapping("/exportZip")
    public void exportZip(@RequestParam List<String> ids, HttpServletResponse response) {
        try {
            prdService.exportAttachmentsAsZip(ids, response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 导出 PRD 清单 Excel
     * <p>
     * 请求：GET /prd/exportExcel
     * <p>
     * 参数：demandName（可选，不传则导出全量）
     * <p>
     * 测试用例：
     * <pre>
     * 1. 按需求名过滤：
     *    GET http://localhost:8080/prd/exportExcel?demandName=测算需求
     *
     * 2. 导出全量：
     *    GET http://localhost:8080/prd/exportExcel
     * </pre>
     * <p>
     * 限流：同一 IP 每分钟最多 5 次
     */
    @RateLimit(rate = 5, interval = 1, key = "exportExcel") // Redisson：Excel 导出限流
    @GetMapping("/exportExcel")
    public void exportExcel(
            @RequestParam(required = false) String demandName,
            HttpServletResponse response) {
        try {
            prdService.exportExcel(demandName, response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== 异步导出 ====================

    /**
     * 异步导出 Excel（立即返回 taskId，后台生成文件）
     * <p>
     * 请求：GET /prd/exportExcel/async
     * <p>
     * Headers：建议带 X-Dept-Id，导出范围与列表机构权限一致
     * <p>
     * 测试流程：
     * <pre>
     * 1. GET  http://localhost:8080/prd/exportExcel/async
     *    → 返回 taskId
     * 2. GET  http://localhost:8080/prd/export/task/{taskId}
     *    → 轮询 status 直到 DONE
     * 3. GET  http://localhost:8080/prd/export/download/{taskId}
     *    → 下载文件
     * </pre>
     */
    @RateLimit(rate = 5, interval = 1, key = "exportExcelAsync")
    @GetMapping("/exportExcel/async")
    public Result<ExportTaskVO> exportExcelAsync(@RequestParam(required = false) String demandName) {
        try {
            return Result.success(exportTaskService.submitExcelAsync(demandName));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 异步打包 ZIP
     * <p>
     * GET http://localhost:8080/prd/exportZip/async?ids={id1},{id2}
     */
    @RateLimit(rate = 5, interval = 1, key = "exportZipAsync")
    @GetMapping("/exportZip/async")
    public Result<ExportTaskVO> exportZipAsync(@RequestParam List<String> ids) {
        try {
            return Result.success(exportTaskService.submitZipAsync(ids));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询异步导出任务状态
     * <p>
     * GET http://localhost:8080/prd/export/task/{taskId}
     */
    @GetMapping("/export/task/{taskId}")
    public Result<ExportTaskVO> exportTaskStatus(@PathVariable String taskId) {
        try {
            return Result.success(exportTaskService.getTask(taskId));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 下载异步导出结果（仅 status=DONE 时可下载）
     * <p>
     * GET http://localhost:8080/prd/export/download/{taskId}
     */
    @GetMapping("/export/download/{taskId}")
    public void exportDownload(@PathVariable String taskId, HttpServletResponse response) {
        try {
            exportTaskService.downloadTask(taskId, response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
