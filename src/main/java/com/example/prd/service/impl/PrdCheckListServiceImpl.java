/**
 * @description: 银行投产材料管理系统 - 核心业务逻辑实现
 * @author: LLQ (Researcher @ SCU-CEIE)
 * @date: 2026-03
 * @version: 1.0
 * @note: 本项目仅用于个人技术方案复现与脱敏展示，严禁用于其他任何用途。
 */

package com.example.prd.service.impl;

import com.alibaba.excel.EasyExcel;
import com.example.prd.entity.PrdCheckList;
import com.example.prd.mapper.PrdCheckListMapper;
import com.example.prd.service.PrdCheckListService;
import com.example.prd.utils.ZipUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 产品核对清单业务实现类
 * 处理业务逻辑、文件存储及格式转换
 */
@Service
public class PrdCheckListServiceImpl implements PrdCheckListService {

    @Autowired
    private PrdCheckListMapper prdMapper;

    /**
     * 从配置文件读取上传根路径，若未配置则默认为 D:/uploads/
     */
    @Value("${file.upload-path:D:/uploads/}")
    private String uploadRootPath;

    /**
     * 保存或更新核对记录
     * 策略：ID为空则视为“新增”，手动生成UUID；ID不为空则视为“修改”，执行选择性更新。
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务，遇异常自动回滚
    public boolean saveWithCheck(PrdCheckList entity) {
        if (entity.getId() == null || entity.getId().trim().isEmpty()) {
            // 生成 32 位唯一标识符作为主键
            entity.setId(UUID.randomUUID().toString().replace("-", ""));
            return prdMapper.insertSelective(entity) > 0;
        } else {
            return prdMapper.updateByPrimaryKeySelective(entity) > 0;
        }
    }


    @Override
    public PrdCheckList getById(String id) {
        return prdMapper.selectByPrimaryKey(id);
    }

    @Override
    public List<PrdCheckList> listByWindowId(String windowVerId) {
        return prdMapper.selectByWindowId(windowVerId);
    }

    /**
     * 多文件异步上传逻辑
     * 1. 自动按日期生成子目录（yyyy/MM/dd）。
     * 2. 执行后缀名过滤与大小限制校验。
     * 3. 文件名加短UUID前缀防止同名冲突。
     * @return 拼接后的相对路径字符串，多个以逗号分隔，用于存入数据库
     */
    @Override
    public String uploadFiles(MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) return null;

        List<String> savedPaths = new ArrayList<>();
        // 构建日期文件夹路径，如 2024/05/20/
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/"));
        File targetDir = new File(uploadRootPath + datePath);

        if (!targetDir.exists()) targetDir.mkdirs(); // 递归创建目录

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();

            // 安全检查：文件后缀校验
            if (fileName != null && !fileName.endsWith(".sql") && !fileName.endsWith(".pdf") && !fileName.endsWith(".zip")) {
                throw new RuntimeException("文件类型非法，仅支持 .sql, .pdf, .zip");
            }

            // 容量检查：单文件上限 10MB
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new RuntimeException("上传失败：文件【" + fileName + "】超过 10MB");
            }

            // 生成保存文件名：前 10 位随机码 + 原文件名
            String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            String saveName = shortUuid + "_" + fileName;

            File dest = new File(targetDir, saveName);
            file.transferTo(dest); // 物理写入硬盘

            // 存入列表，随后用逗号合并成一个字符串返回
            savedPaths.add(datePath + saveName);
        }
        return String.join(",", savedPaths);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 事务管理：若数据库更新失败，可手动处理或回滚
    public String uploadAndBind(MultipartFile[] files, String id) throws IOException {
        // 1. 调用现有的 uploadFiles 方法处理物理文件存储
        // 返回格式如：2026/xx/xx/uuid_a.pdf,2026/xx/xx/uuid_b.sql
        String newPaths = this.uploadFiles(files);

        if (newPaths == null || newPaths.isEmpty()) {
            return null;
        }

        // 2. 检查数据库中是否存在该 ID 的记录
        PrdCheckList existRecord = prdMapper.selectByPrimaryKey(id);
        if (existRecord == null) {
            throw new RuntimeException("关联失败：找不到 ID 为 [" + id + "] 的数据记录");
        }

        // 3. 路径合并逻辑（防止覆盖旧附件）
        String finalPath;
        String oldPath = existRecord.getAttachmentPath();

        if (oldPath != null && !oldPath.trim().isEmpty()) {
            // 如果原本就有文件，用逗号拼接新路径
            finalPath = oldPath + "," + newPaths;
        } else {
            // 如果原本没文件，直接使用新路径
            finalPath = newPaths;
        }

        // 4. 执行数据库更新
        PrdCheckList updateEntity = new PrdCheckList();
        updateEntity.setId(id);
        updateEntity.setAttachmentPath(finalPath);
        // updateEntity.setUpdateUser("ADMIN"); // 建议记录更新人

        int rows = prdMapper.updateByPrimaryKeySelective(updateEntity);
        if (rows <= 0) {
            throw new RuntimeException("数据库更新失败，请检查 ID 是否正确");
        }

        return finalPath;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAttachment(String id) {
        PrdCheckList entity = prdMapper.selectByPrimaryKey(id);
        if (entity == null || entity.getAttachmentPath() == null || entity.getAttachmentPath().isEmpty()) {
            return true;
        }

        // 1. 物理删除磁盘文件
        String[] paths = entity.getAttachmentPath().split(",");
        for (String p : paths) {
            File file = new File(uploadRootPath + p);
            if (file.exists()) {
                file.delete(); // 真正从硬盘抹除
            }
        }

        // 2. 清空数据库字段
        PrdCheckList updateNode = new PrdCheckList();
        updateNode.setId(id);
        updateNode.setAttachmentPath(""); // 置空
        return prdMapper.updateByPrimaryKeySelective(updateNode) > 0;
    }
    /**
     * 导出 Excel 报表
     * 使用 EasyExcel 框架，在流式写入前对数据库中的状态码（0/1）进行文字转换。
     */
    @Override
    public void exportExcel(String demandName, HttpServletResponse response) throws IOException {
        // 1. 获取全量待导出数据
        List<PrdCheckList> data = prdMapper.selectAll(demandName);

        // 2. 数据清洗：将数据库存储的数字标识转换为人类可读的“通过/未通过”
        for (PrdCheckList item : data) {
            item.setUatEnvCheck(transferStatus(item.getUatEnvCheck()));
            item.setProdEnvCheck(transferStatus(item.getProdEnvCheck()));
        }

        // 3. 配置 HTTP 响应头（Excel 专用意图）
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = "PRD_Export_" + System.currentTimeMillis() + ".xlsx";
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        // 4. 执行写入并关闭流
        EasyExcel.write(response.getOutputStream(), PrdCheckList.class)
                .sheet("PRD核对清单")
                .doWrite(data);
    }

    /**
     * 状态转换私有方法：处理业务显示逻辑
     */
    private String transferStatus(String status) {
        if ("1".equals(status)) return "通过";
        if ("0".equals(status)) return "未通过";
        return status;
    }

    /**
     * 附件打包下载逻辑
     * 将多条记录、多个附件路径汇总，统一打成一个 ZIP 包供用户下载。
     */
    @Override
    public void exportAttachmentsAsZip(List<String> ids, HttpServletResponse response) throws IOException {
        List<File> allFiles = new ArrayList<>();

        for (String id : ids) {
            PrdCheckList prd = prdMapper.selectByPrimaryKey(id);
            // 严谨校验：确保记录存在且确实含有附件
            if (prd != null && prd.getAttachmentPath() != null) {
                // 数据库中存储格式为 "path1,path2"，需拆分处理
                String[] paths = prd.getAttachmentPath().split(",");
                for (String p : paths) {
                    // 拼接物理全路径并核实硬盘上是否存在该文件
                    File f = new File(uploadRootPath + p);
                    if (f.exists()) {
                        allFiles.add(f);
                    }
                }
            }
        }

        // 调用 ZIP 压缩工具类，将 File 集合推送到 response 输出流
        ZipUtils.downloadZip(allFiles, response);
    }

    @Autowired
    private SysDeptServiceImpl sysDeptService; // 注入部门服务
    /**
     * 分页查询
     * @param current 当前页码
     * @param size 每页条数
     * @param demandName 需求名称（模糊查询条件）
     * 修改后的分页查询：整合 Redis 加速的部门 ID 获取逻辑
     */
    @Override
    public List<PrdCheckList> selectCustomPage(int current, int size, String demandName, Long deptId, boolean recursive) {
        // 计算 SQL 的 LIMIT 偏移量
        long offset = (long) (current - 1) * size;

        List<Long> deptIds = null;

        if (deptId != null) {
            if (recursive) {
                // 【加速点】：这里调用的 selectChildrenIds 内部已经实现了 Redis 缓存
                // 第一次调用会算递归并存 Redis，之后 24 小时内直接走 Redis，不再损耗 CPU 算递归
                deptIds = sysDeptService.selectChildrenIds(deptId);
            } else {
                // 非递归模式，直接放当前部门 ID
                deptIds = new ArrayList<>();
                deptIds.add(deptId);
            }
        }

        // 调用 Mapper，执行带 IN 子句的查询
        return prdMapper.selectByCondition(demandName, deptIds, offset, size);
    }


}