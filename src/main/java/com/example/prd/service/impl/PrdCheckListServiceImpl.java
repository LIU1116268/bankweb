package com.example.prd.service.impl;

import com.alibaba.excel.EasyExcel;
import com.example.prd.entity.PrdCheckList;
import com.example.prd.mapper.PrdCheckListMapper;
import com.example.prd.service.PrdCheckListService;
import com.example.prd.service.SysDeptService;
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

/**
 * PRD 投产检查清单 - 业务实现
 * <p>
 * 职责：清单持久化、分页检索、附件存储、Excel/ZIP 导出
 */
@Service
public class PrdCheckListServiceImpl implements PrdCheckListService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    @Autowired
    private PrdCheckListMapper prdMapper;

    @Autowired
    private SysDeptService sysDeptService;

    @Value("${file.upload-path:D:/uploads/}")
    private String uploadRootPath;

    // ==================== 清单 CRUD ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveWithCheck(PrdCheckList entity) {
        if (entity.getId() == null || entity.getId().trim().isEmpty()) {
            entity.setId(UUID.randomUUID().toString().replace("-", ""));
            return prdMapper.insertSelective(entity) > 0;
        }
        return prdMapper.updateByPrimaryKeySelective(entity) > 0;
    }

    @Override
    public PrdCheckList getById(String id) {
        return prdMapper.selectByPrimaryKey(id);
    }

    @Override
    public List<PrdCheckList> selectCustomPage(
            int current, int size, String demandName, Long deptId, boolean recursive) {
        long offset = (long) (current - 1) * size;
        List<Long> deptIds = null;

        if (deptId != null) {
            if (recursive) {
                // 递归子部门 ID 已由 SysDeptService 做 Redis 缓存
                deptIds = sysDeptService.selectChildrenIds(deptId);
            } else {
                deptIds = new ArrayList<>();
                deptIds.add(deptId);
            }
        }
        return prdMapper.selectByCondition(demandName, deptIds, offset, size);
    }

    // ==================== 附件管理 ====================

    @Override
    public String uploadFiles(MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            return null;
        }

        List<String> savedPaths = new ArrayList<>();
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/"));
        File targetDir = new File(uploadRootPath + datePath);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            validateFile(fileName, file.getSize());

            String saveName = UUID.randomUUID().toString().replace("-", "").substring(0, 10) + "_" + fileName;
            file.transferTo(new File(targetDir, saveName));
            savedPaths.add(datePath + saveName);
        }
        return String.join(",", savedPaths);
    }

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

        String oldPath = existRecord.getAttachmentPath();
        String finalPath = (oldPath != null && !oldPath.trim().isEmpty())
                ? oldPath + "," + newPaths
                : newPaths;

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

        for (String p : entity.getAttachmentPath().split(",")) {
            File file = new File(uploadRootPath + p);
            if (file.exists()) {
                file.delete();
            }
        }

        PrdCheckList updateNode = new PrdCheckList();
        updateNode.setId(id);
        updateNode.setAttachmentPath("");
        return prdMapper.updateByPrimaryKeySelective(updateNode) > 0;
    }

    // ==================== 导出 ====================

    @Override
    public void exportAttachmentsAsZip(List<String> ids, HttpServletResponse response) throws IOException {
        List<File> allFiles = new ArrayList<>();
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

    @Override
    public void exportExcel(String demandName, HttpServletResponse response) throws IOException {
        List<PrdCheckList> data = prdMapper.selectAll(demandName);
        for (PrdCheckList item : data) {
            item.setUatEnvCheck(transferStatus(item.getUatEnvCheck()));
            item.setProdEnvCheck(transferStatus(item.getProdEnvCheck()));
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = "PRD_Export_" + System.currentTimeMillis() + ".xlsx";
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

    /** 将库中 0/1 状态码转为导出用中文 */
    private String transferStatus(String status) {
        if ("1".equals(status)) {
            return "通过";
        }
        if ("0".equals(status)) {
            return "未通过";
        }
        return status;
    }
}
