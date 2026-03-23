package com.example.prd.service.impl;

import com.example.prd.entity.SysDept;
import com.example.prd.mapper.SysDeptMapper;
import com.example.prd.service.SysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SysDeptServiceImpl implements SysDeptService {

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_TREE = "bankweb:dept:tree";
    private static final String CACHE_KEY_CHILDREN = "bankweb:dept:childrenIds:";

    @Override
    public List<SysDept> selectDeptList(SysDept dept) {
        return deptMapper.selectDeptList(dept);
    }

    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts) {
        // 1. 尝试从缓存获取
        List<SysDept> cachedTree = (List<SysDept>) redisTemplate.opsForValue().get(CACHE_KEY_TREE);
        if (cachedTree != null) {
            return cachedTree;
        }

        // 2. 缓存没有，执行原有递归构建逻辑
        List<SysDept> tree = depts.stream()
                .filter(d -> d.getParentId() == 0)
                .map(d -> {
                    d.setChildren(getChildren(d, depts));
                    return d;
                }).collect(Collectors.toList());

        // 3. 写入缓存，设置 24 小时过期
        redisTemplate.opsForValue().set(CACHE_KEY_TREE, tree, 24, TimeUnit.HOURS);
        return tree;
    }

    @Override
    public List<Long> selectChildrenIds(Long deptId) {
        String key = "bankweb:dept:childrenIds:" + deptId;

        // 1. 先去 Redis 拿
        List<Long> cachedIds = (List<Long>) redisTemplate.opsForValue().get(key);
        if (cachedIds != null) {
            return cachedIds; // 命中缓存，直接返回，毫秒级响应
        }

        // 2. 缓存没中，再算递归
        List<Long> ids = new ArrayList<>();
        List<SysDept> all = deptMapper.selectDeptList(new SysDept());
        ids.add(deptId);
        fillChildIds(all, ids, deptId); // 递归算法

        // 3. 存入 Redis，设置过期时间
        redisTemplate.opsForValue().set(key, ids, 24, TimeUnit.HOURS);

        return ids;
    }

    @Override
    public void clearDeptCache() {
        // 清理整棵树的缓存
        redisTemplate.delete(CACHE_KEY_TREE);
        // 清理所有子部门集合缓存（匹配前缀）
        java.util.Set<String> keys = redisTemplate.keys(CACHE_KEY_CHILDREN + "*");
        if (keys != null) redisTemplate.delete(keys);
    }

    @Override
    @Transactional
    public int insertDept(SysDept dept) {
        int rows = deptMapper.insert(dept); // 直接调用 MyBatis-Plus 提供的 baseMapper.insert()
        if (rows > 0) {
            clearDeptCache(); // 插入成功后立即清理缓存，保证下次查询是最新数据
        }
        return rows;
    }

    // 辅助方法：递归填充子部门
    private List<SysDept> getChildren(SysDept parent, List<SysDept> all) {
        return all.stream()
                .filter(d -> d.getParentId().equals(parent.getDeptId()))
                .map(d -> {
                    d.setChildren(getChildren(d, all));
                    return d;
                }).collect(Collectors.toList());
    }

    private void fillChildIds(List<SysDept> all, List<Long> ids, Long parentId) {
        for (SysDept d : all) {
            if (d.getParentId().equals(parentId)) {
                ids.add(d.getDeptId());
                fillChildIds(all, ids, d.getDeptId());
            }
        }
    }
}