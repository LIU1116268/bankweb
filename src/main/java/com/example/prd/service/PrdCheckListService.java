package com.example.prd.service;

import com.example.prd.entity.PrdCheckList;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * PRD 投产检查清单 - 业务接口
 */
public interface PrdCheckListService {

    /** 新增或更新（id 为空则新增） */
    boolean saveWithCheck(PrdCheckList entity);

    /** 按主键查询 */
    PrdCheckList getById(String id);

    /** 分页条件查询（支持部门递归范围） */
    List<PrdCheckList> selectCustomPage(int current, int size, String demandName, Long deptId, boolean recursive);

    /** 多文件上传，返回逗号分隔的相对路径 */
    String uploadFiles(MultipartFile[] files) throws IOException;

    /** 上传并绑定到指定记录 */
    String uploadAndBind(MultipartFile[] files, String id) throws IOException;

    /** 删除记录关联的全部附件 */
    boolean deleteAttachment(String id);

    /** 按记录 ID 列表打包 ZIP 下载 */
    void exportAttachmentsAsZip(List<String> ids, HttpServletResponse response) throws IOException;

    /** 导出 Excel */
    void exportExcel(String demandName, HttpServletResponse response) throws IOException;
}
