package com.example.prd.service.impl;

import com.example.prd.context.DataScopeContext;
import com.example.prd.entity.PrdCheckList;
import com.example.prd.exception.DataScopeException;
import com.example.prd.mapper.PrdCheckListMapper;
import com.example.prd.service.DataScopeService;
import com.example.prd.service.SysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 机构数据权限实现
 * <p>
 * 规则：
 * <ul>
 *   <li>未传 X-Dept-Id：不做机构过滤（兼容本地调试）</li>
 *   <li>已传 X-Dept-Id：只能看「本机构 + 全部下级机构」的数据</li>
 *   <li>若额外传 queryDeptId，必须在上述范围内，否则 403</li>
 * </ul>
 */
@Service
public class DataScopeServiceImpl implements DataScopeService {

    @Autowired
    private SysDeptService sysDeptService;

    @Autowired
    private PrdCheckListMapper prdMapper;

    @Override
    public List<Long> resolveDeptFilter(Long queryDeptId, boolean recursive) {
        Long userDeptId = DataScopeContext.getDeptId();
        List<Long> scopeIds = userDeptId == null ? null : sysDeptService.selectChildrenIds(userDeptId);

        if (queryDeptId == null) {
            // 未指定查询部门：默认用当前用户机构范围
            return scopeIds;
        }

        if (scopeIds != null && !scopeIds.contains(queryDeptId)) {
            throw new DataScopeException("无权查看机构 [" + queryDeptId + "] 的数据，您所属机构为 ["
                    + userDeptId + "] 及其下级");
        }

        if (recursive) {
            return sysDeptService.selectChildrenIds(queryDeptId);
        }
        List<Long> single = new ArrayList<>();
        single.add(queryDeptId);
        return single;
    }

    @Override
    public void checkRecordAccess(PrdCheckList record) {
        if (record == null || !DataScopeContext.isEnabled()) {
            return;
        }
        if (record.getDeptId() == null) {
            return;
        }
        Long userDeptId = DataScopeContext.getDeptId();
        List<Long> scopeIds = sysDeptService.selectChildrenIds(userDeptId);
        if (!scopeIds.contains(record.getDeptId())) {
            throw new DataScopeException("无权访问该记录，记录所属机构 [" + record.getDeptId()
                    + "] 不在您的数据范围内");
        }
    }

    @Override
    public void checkRecordIdsAccess(List<String> ids) {
        if (ids == null || !DataScopeContext.isEnabled()) {
            return;
        }
        for (String id : ids) {
            checkRecordAccess(prdMapper.selectByPrimaryKey(id));
        }
    }
}
