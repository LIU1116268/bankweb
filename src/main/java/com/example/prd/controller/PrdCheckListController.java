package com.example.prd.controller;

import com.example.prd.annotation.Log;
import com.example.prd.common.Result;
import com.example.prd.entity.PrdCheckList;
import com.example.prd.service.PrdCheckListService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 产品核对清单前端控制器
 * 提供 CRUD 接口、文件上传、Excel 导出及附件打包功能
 */
@RestController
@RequestMapping("/prd")
public class PrdCheckListController {

    @Autowired
    private PrdCheckListService prdService;

    /**
     * 新增或更新记录
     * 逻辑：对象 ID 为空则新增，不为空则更新。
     * 请求方式：POST
     * 测试 URL: http://localhost:8080/prd/save
     * 参数示例：JSON 格式的 PrdCheckList 对象
     * {
     *     "windowVerId": "202603251",
     *     "demandName": "信用卡-分期额度动态测算",
     *     "prodContent": "前端UI适配-H9",
     *     "relaFeature": "feature-card-limit-02",
     *     "relaScript": "不涉及",
     *     "prodType": "功能新增",
     *     "demandManager": "范小凡",
     *     "techManager": "黄小布",
     *     "uatEnvCheck": "已通过",
     *     "prodEnvCheck": "待核对",
     *     "remark": "",
     *     "createUser": "104356",
     * }
     */
    @Log(title = "核对清单", businessType = "SAVE")
    @PostMapping("/save")
    public Result save(@RequestBody PrdCheckList prd) {
        return prdService.saveWithCheck(prd) ? Result.success() : Result.error("保存失败");
    }

    /**
     * 分页查询列表（支持按需求名称模糊搜索）
     * 请求方式：GET
     * 测试示例：
     * 1. 基础分页：http://localhost:8080/prd/list?current=1&size=5
     * 2. 条件搜索：http://localhost:8080/prd/list?current=1&size=5&demandName=信用卡
     * * @param current 当前页码，默认 1  不传入就默认
     * @param size    每页条数，默认 5 最多就显示满足条件的5条
     * @param demandName 搜索关键词（非必传）
     * 查成都分行及下属所有支行（默认行为）：
     * http://localhost:8080/prd/list?deptId=101
     * 只查成都分行本级的数据（不看支行）：
     * http://localhost:8080/prd/list?deptId=101&recursive=false
     */
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int current,
                       @RequestParam(defaultValue = "5") int size,
                       @RequestParam(required = false) String demandName,
                       @RequestParam(required = false) Long deptId,
                       @RequestParam(defaultValue = "true") boolean recursive) { //是否递归
        return Result.success(prdService.selectCustomPage(current, size, demandName, deptId, recursive));
    }

    /**
     * 根据主键 ID 获取详情
     * 请求方式：GET
     * 测试 http://localhost:8080/prd/detail/PCL2026031800000003
     */
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable String id) {
        return Result.success(prdService.getById(id));
    }


    /**
     * 多文件上传接口
     * 请求方式：POST (Content-Type: multipart/form-data),在api里面选择
     * 参数名：files (可选择多个文件同步上传)
     * 参数类型file
     * 返回值：成功后返回在服务器存储的相对路径字符串（逗号分隔）
     * upload-path: D:/prd_attachments/  # 上传文件存放的根路径
     * 先上传、后提交，根据上传返回到路径填写表格数据
     */
    @Log(title = "文件上传", businessType = "UPLOAD")
    @PostMapping("/upload")
    // @RequestParam("files") 去前端找名叫 files 的参数
    // MultipartFile[] getfiles 后端用数组接收
    // 前端传过来一组名叫 files 的文件 → 后端用数组接收
    public Result upload(@RequestParam("files") MultipartFile[] getfiles) {
        try {
            String pathString = prdService.uploadFiles(getfiles);
            return Result.success(pathString);
        } catch (Exception e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 关联上传接口
     * 请求示例：POST http://localhost:8080/prd/uploadbind?id=PCL2026031800000004
     * 注意：需使用 multipart/form-data 格式发送文件
     * 适用于“对已有数据补录附件”。
     * http://localhost:8080/sysLog/list
     */
    @Log(title = "关联附件上传", businessType = "UPLOADBIND")
    @PostMapping("/uploadBind")
    public Result upload(@RequestParam("files") MultipartFile[] files,
                         @RequestParam("id") String id) { // 接收前端传来的 id
        try {
            // 执行“上传+绑定”业务
            String finalPath = prdService.uploadAndBind(files, id);
            return Result.success("文件已成功关联至记录：" + id, finalPath);
        } catch (RuntimeException e) {
            // 捕获业务异常（如 ID 不存在）
            return Result.error(e.getMessage());
        } catch (Exception e) {
            // 捕获系统异常（如读写错误）
            return Result.error("服务器内部错误：" + e.getMessage());
        }
    }

    @Log(title = "删除附件", businessType = "DELETE")
    @DeleteMapping("/deleteFile/{id}")
    public Result deleteFile(@PathVariable String id) {
        return prdService.deleteAttachment(id) ? Result.success("附件已清理") : Result.error("清理失败");
    }

    /**
     * 批量导出附件并压缩为 ZIP 包
     * 请求方式：GET
     * 测试 URL: http://localhost:8080/prd/exportZip?ids=PCL2026031800000004,PCL2026031800000003
     * 注意：直接通过浏览器访问会触发文件下载。
     */
    @GetMapping("/exportZip")
    public void export(@RequestParam List<String> ids, HttpServletResponse response) {
        try {
            prdService.exportAttachmentsAsZip(ids, response);
        } catch (IOException e) {
            // 异常通常在写入 Response 流时发生
            e.printStackTrace();
        }
    }

    /**
     * 导出 Excel 报表接口
     * 请求方式：GET
     * 测试 URL: http://localhost:8080/prd/exportExcel?demandName=测算需求
     * 说明：如果不传 demandName，则导出全量数据。
     */
    @GetMapping("/exportExcel")
    public void exportExcel(@RequestParam(required = false) String demandName, HttpServletResponse response) {
        try {
            prdService.exportExcel(demandName, response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}