package com.example.prd.service.impl;

import com.alibaba.excel.EasyExcel;
import com.example.prd.context.DataScopeContext;
import com.example.prd.dto.PrdStatusTransitionRequest;
import com.example.prd.entity.PrdCheckList;
import com.example.prd.enums.PrdCheckStatus;
import com.example.prd.exception.PrdStatusException;
import com.example.prd.mapper.PrdCheckListMapper;
import com.example.prd.service.DataScopeService;
import com.example.prd.service.PrdCheckListService;
import com.example.prd.vo.PrdStatusTransitionVO;
import com.example.prd.utils.ZipUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PRD 投产检查清单 - 业务实现
 * <p>
 * 职责：清单持久化、分页检索、附件存储、Excel/ZIP 导出、投产状态机流转
 */
@Service
public class PrdCheckListServiceImpl implements PrdCheckListService {

    /** 单文件大小上限 10MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    @Autowired
    private PrdCheckListMapper prdMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Value("${file.upload-path:D:/uploads/}")
    private String uploadRootPath;

    // ==================== 清单 CRUD ====================

    /**
     * 保存或更新核对记录
     * 策略：ID 为空则新增（手动生成 32 位 UUID）；ID 不为空则选择性更新
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务，遇异常自动回滚
    public boolean saveWithCheck(PrdCheckList entity) {
        if (entity.getId() == null || entity.getId().trim().isEmpty()) {
            entity.setId(UUID.randomUUID().toString().replace("-", ""));
            // 新增记录默认进入「草稿」，后续通过 /prd/transition 推进流程
            if (entity.getStatus() == null || entity.getStatus().isBlank()) {
                entity.setStatus(PrdCheckStatus.DRAFT.getCode());
            }
            // 未传 deptId 时，默认归属当前用户机构（需请求头 X-Dept-Id）
            if (entity.getDeptId() == null && DataScopeContext.isEnabled()) {
                entity.setDeptId(DataScopeContext.getDeptId());
            }
            return prdMapper.insertSelective(entity) > 0;
        }
        // 更新前校验机构权限
        PrdCheckList exist = prdMapper.selectByPrimaryKey(entity.getId());
        if (exist == null) {
            throw new RuntimeException("找不到 ID 为 [" + entity.getId() + "] 的数据记录");
        }
        dataScopeService.checkRecordAccess(exist);
        // 更新时禁止通过 save 接口直接改状态，避免绕过状态机
        entity.setStatus(null);
        return prdMapper.updateByPrimaryKeySelective(entity) > 0;
    }

    @Override
    public PrdCheckList getById(String id) {
        PrdCheckList record = prdMapper.selectByPrimaryKey(id);
        dataScopeService.checkRecordAccess(record);
        fillStatusLabel(record);
        return record;
    }

    // ==================== 状态机 ====================

    /**
     * PRD 投产检查状态机 - 核心流转逻辑
     * <p>
     * 1. 查库得到当前状态<br>
     * 2. 用 {@link PrdCheckStatus#canTransitTo} 校验是否允许跳转<br>
     * 3. 仅更新 status / updateUser / remark<br>
     * 4. Controller 上的 @Log 会把本次请求参数与结果写入 sys_oper_log，实现可追溯
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrdStatusTransitionVO transitionStatus(PrdStatusTransitionRequest request) {
        if (request == null || request.getId() == null || request.getId().isBlank()) {
            throw new PrdStatusException("记录 ID 不能为空");
        }
        if (request.getTargetStatus() == null || request.getTargetStatus().isBlank()) {
            throw new PrdStatusException("目标状态 targetStatus 不能为空");
        }

        PrdCheckList exist = prdMapper.selectByPrimaryKey(request.getId());
        if (exist == null) {
            throw new PrdStatusException("找不到 ID 为 [" + request.getId() + "] 的清单记录");
        }
        dataScopeService.checkRecordAccess(exist);

        PrdCheckStatus current = resolveCurrentStatus(exist.getStatus());
        PrdCheckStatus target = PrdCheckStatus.fromCode(request.getTargetStatus());
        if (target == null) {
            throw new PrdStatusException("非法目标状态：" + request.getTargetStatus());
        }
        if (!current.canTransitTo(target)) {
            throw new PrdStatusException(String.format(
                    "不允许从【%s】流转到【%s】，当前允许的目标：%s",
                    current.getLabel(),
                    target.getLabel(),
                    current.allowedNextStatuses().stream()
                            .map(PrdCheckStatus::getLabel)
                            .collect(Collectors.joining("、"))));
        }

        PrdCheckList update = new PrdCheckList();
        update.setId(request.getId());
        update.setStatus(target.getCode());
        if (request.getOperator() != null && !request.getOperator().isBlank()) {
            update.setUpdateUser(request.getOperator());
        }
        if (request.getRemark() != null) {
            update.setRemark(request.getRemark());
        }
        if (prdMapper.updateByPrimaryKeySelective(update) <= 0) {
            throw new PrdStatusException("状态更新失败");
        }

        PrdStatusTransitionVO vo = new PrdStatusTransitionVO();
        vo.setId(request.getId());
        vo.setFromStatus(current.getCode());
        vo.setFromStatusLabel(current.getLabel());
        vo.setToStatus(target.getCode());
        vo.setToStatusLabel(target.getLabel());
        return vo;
    }

    @Override
    public PrdStatusTransitionVO getAllowedTransitions(String id) {
        PrdCheckList exist = prdMapper.selectByPrimaryKey(id);
        if (exist == null) {
            throw new PrdStatusException("找不到 ID 为 [" + id + "] 的清单记录");
        }
        dataScopeService.checkRecordAccess(exist);
        PrdCheckStatus current = resolveCurrentStatus(exist.getStatus());

        PrdStatusTransitionVO vo = new PrdStatusTransitionVO();
        vo.setId(id);
        vo.setFromStatus(current.getCode());
        vo.setFromStatusLabel(current.getLabel());
        vo.setAllowedNextStatuses(current.allowedNextStatuses().stream()
                .map(PrdCheckStatus::getCode)
                .collect(Collectors.toList()));
        vo.setAllowedNextStatusLabels(current.allowedNextStatuses().stream()
                .map(PrdCheckStatus::getLabel)
                .collect(Collectors.toList()));
        return vo;
    }

    /**
     * 分页查询（整合 Redis 加速的部门 ID 获取）
     * <p>
     * offset = (current - 1) * size，即 SQL LIMIT 要跳过的行数
     */
    @Override
    public List<PrdCheckList> selectCustomPage(
            int current, int size, String demandName, Long deptId, boolean recursive) {
        long offset = (long) (current - 1) * size;
        // 机构数据权限：结合 X-Dept-Id 与可选 query deptId
        List<Long> deptIds = dataScopeService.resolveDeptFilter(deptId, recursive);
        List<PrdCheckList> list = prdMapper.selectByCondition(demandName, deptIds, offset, size);
        list.forEach(this::fillStatusLabel);
        return list;
    }

    // ==================== 附件管理 ====================

    /**
     * 多文件上传
     * <ol>
     *   <li>按日期分子目录 yyyy/MM/dd/</li>
     *   <li>校验后缀与 10MB 大小</li>
     *   <li>文件名加短 UUID 前缀，防止同名覆盖</li>
     * </ol>
     * 返回逗号分隔的相对路径，供存入 attachment_path 字段
     */
    @Override
    public String uploadFiles(MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            return null;
        }

        List<String> savedPaths = new ArrayList<>();
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/"));
        File targetDir = new File(uploadRootPath + datePath);
        if (!targetDir.exists()) {
            targetDir.mkdirs(); // 递归创建目录
        }

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            validateFile(fileName, file.getSize());

            // 前 10 位随机码 + 原文件名
            String saveName = UUID.randomUUID().toString().replace("-", "").substring(0, 10) + "_" + fileName;
            File dest = new File(targetDir, saveName);
            file.transferTo(dest); // 物理写入硬盘

            savedPaths.add(datePath + saveName);
        }
        return String.join(",", savedPaths);
    }

    /**
     * 上传文件并绑定到指定业务记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAndBind(MultipartFile[] files, String id) throws IOException {
        String newPaths = uploadFiles(files);
        if (newPaths == null || newPaths.isEmpty()) {
            return null;
        }

        PrdCheckList existRecord = prdMapper.selectByPrimaryKey(id);
        if (existRecord == null) {
            throw new RuntimeException("关联失败：找不到 ID 为 [" + id + "] 的数据记录");
        }
        dataScopeService.checkRecordAccess(existRecord);

        // 路径合并：原有附件不覆盖，用逗号拼接
        String oldPath = existRecord.getAttachmentPath();
        String finalPath = (oldPath != null && !oldPath.trim().isEmpty())
                ? oldPath + "," + newPaths
                : newPaths;

        // 只更新有值的字段：new 空壳对象 + set 主键和附件路径
        PrdCheckList updateEntity = new PrdCheckList();
        updateEntity.setId(id);
        updateEntity.setAttachmentPath(finalPath);

        if (prdMapper.updateByPrimaryKeySelective(updateEntity) <= 0) {
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
        dataScopeService.checkRecordAccess(entity);

        // 1. 物理删除磁盘文件（库中路径可能是 path1,path2）
        for (String p : entity.getAttachmentPath().split(",")) {
            File file = new File(uploadRootPath + p);
            if (file.exists()) {
                file.delete();
            }
        }

        // 2. 清空数据库 attachment_path 字段
        PrdCheckList updateNode = new PrdCheckList();
        updateNode.setId(id);
        updateNode.setAttachmentPath("");
        return prdMapper.updateByPrimaryKeySelective(updateNode) > 0;
    }

    // ==================== 导出 ====================

    /**
     * 将多条记录的附件打包为一个 ZIP 供下载
     */
    @Override
    public void exportAttachmentsAsZip(List<String> ids, HttpServletResponse response) throws IOException {
        List<File> allFiles = new ArrayList<>();
        dataScopeService.checkRecordIdsAccess(ids);
        for (String id : ids) {
            PrdCheckList prd = prdMapper.selectByPrimaryKey(id);
            if (prd == null || prd.getAttachmentPath() == null) {
                continue;
            }
            for (String p : prd.getAttachmentPath().split(",")) {
                File f = new File(uploadRootPath + p);
                if (f.exists()) {
                    allFiles.add(f);
                }
            }
        }
        ZipUtils.downloadZip(allFiles, response);
    }

    /**
     * 导出 Excel：EasyExcel 流式写入；导出前将库中 0/1 转为「未通过/通过」
     */
    @Override
    public void exportExcel(String demandName, HttpServletResponse response) throws IOException {
        List<Long> deptIds = dataScopeService.resolveDeptFilter(null, true);
        List<PrdCheckList> data = prdMapper.selectAllForExport(demandName, deptIds);
        for (PrdCheckList item : data) {
            item.setUatEnvCheck(transferCheckFlag(item.getUatEnvCheck()));
            item.setProdEnvCheck(transferCheckFlag(item.getProdEnvCheck()));
            fillStatusLabel(item);
            // 导出 Excel 时把 status 列显示为中文
            if (item.getStatusLabel() != null) {
                item.setStatus(item.getStatusLabel());
            }
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = "PRD_Export_" + System.currentTimeMillis() + ".xlsx";
        // attachment 表示下载，而不是在浏览器内嵌预览
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        EasyExcel.write(response.getOutputStream(), PrdCheckList.class)
                .sheet("PRD核对清单")
                .doWrite(data);
    }

    // ==================== 私有方法 ====================

    private void validateFile(String fileName, long size) {
        if (fileName != null
                && !fileName.endsWith(".sql")
                && !fileName.endsWith(".pdf")
                && !fileName.endsWith(".zip")) {
            throw new RuntimeException("文件类型非法，仅支持 .sql, .pdf, .zip");
        }
        if (size > MAX_FILE_SIZE) {
            throw new RuntimeException("上传失败：文件【" + fileName + "】超过 10MB");
        }
    }

    /** UAT/生产检查字段：数据库存 0/1 或中文，导出时统一显示 */
    private String transferCheckFlag(String value) {
        if ("1".equals(value)) {
            return "通过";
        }
        if ("0".equals(value)) {
            return "未通过";
        }
        return value;
    }

    /** 库中无 status 的旧数据，按「已提交」处理，便于继续走流程 */
    private PrdCheckStatus resolveCurrentStatus(String statusCode) {
        PrdCheckStatus status = PrdCheckStatus.fromCode(statusCode);
        if (status == null) {
            return PrdCheckStatus.SUBMITTED;
        }
        return status;
    }

    /** 填充 statusLabel 供前端展示 */
    private void fillStatusLabel(PrdCheckList record) {
        if (record == null) {
            return;
        }
        PrdCheckStatus status = PrdCheckStatus.fromCode(record.getStatus());
        if (status != null) {
            record.setStatusLabel(status.getLabel());
        } else if (record.getStatus() != null) {
            record.setStatusLabel(record.getStatus());
        }
    }
}
