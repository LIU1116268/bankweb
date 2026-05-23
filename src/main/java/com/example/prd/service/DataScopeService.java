package com.example.prd.service;

import com.example.prd.entity.PrdCheckList;

import java.util.List;

/**
 * 机构数据权限服务
 */
public interface DataScopeService {

    /**
     * 解析列表/导出应使用的部门 ID 集合
     *
     * @param queryDeptId 前端传入的 deptId（可为 null）
     * @param recursive   是否包含下级
     */
    List<Long> resolveDeptFilter(Long queryDeptId, boolean recursive);

    /** 校验单条记录是否在当前用户机构范围内 */
    void checkRecordAccess(PrdCheckList record);

    /** 校验多个 ID 是否均可访问（异步 ZIP 导出用） */
    void checkRecordIdsAccess(List<String> ids);
}
