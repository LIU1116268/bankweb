package com.example.prd.service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import com.example.prd.entity.PrdCheckList;

import java.io.IOException;
import java.util.List;

public interface PrdCheckListService {

    // 保存
    boolean saveWithCheck(PrdCheckList entity);

    // 分页查询（手动实现分页，不再返回 MyBatis Plus 的 Page 对象）
    List<PrdCheckList> selectCustomPage(int current, int size, String demandName, Long deptId, boolean recursive);

    // 根据 ID 获取详情
    PrdCheckList getById(String id);

    // 根据版本号查询
    List<PrdCheckList> listByWindowId(String windowVerId);

    String uploadFiles(MultipartFile[] files) throws IOException;

    void exportAttachmentsAsZip(List<String> ids, HttpServletResponse response) throws IOException;

    // 删除/重置附件
    boolean deleteAttachment(String id);

    void exportExcel(String demandName, HttpServletResponse response) throws IOException;

    /**
     * 上传文件并关联到指定 ID 的记录
     * @param files 文件数组
     * @param id 业务主键 ID
     * @return 最终合并后的完整路径字符串
     * @throws IOException
     */
    String uploadAndBind(MultipartFile[] files, String id) throws IOException;


}