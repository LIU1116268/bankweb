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
    // 一整棵完整的部门树的键bankweb:dept:tree
    public List<SysDept> buildDeptTree(List<SysDept> depts) {
        List<SysDept> cachedTree = (List<SysDept>) redisTemplate.opsForValue().get(CACHE_KEY_TREE);
        if (cachedTree != null) {
            return cachedTree;
        }
        // 2. 缓存没有，执行原有递归构建逻辑
        List<SysDept> tree = depts.stream() // 把（所有部门）变成 流水线
                // 只挑出那些没有上级（parentId 为 0）的顶级部门
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
                    return d;// filter map 同时执行
                }).collect(Collectors.toList());
    }


    @Override
    public List<Long> selectChildrenIds(Long deptId) {
        String key = "bankweb:dept:childrenIds:" + deptId;// 每个部门key不同

        // 1. 先去 Redis 拿
        // 当前部门自己的 ID + 所有下级子部门、孙部门的 ID 列表
        List<Long> cachedIds = (List<Long>) redisTemplate.opsForValue().get(key);
        if (cachedIds != null) {
            return cachedIds;
        }
        // 2. 缓存没中，再算递归
        // 定义结果列表
        List<Long> ids = new ArrayList<>();
        // 获取数据库里的【所有部门】的完整列表
        List<SysDept> all = deptMapper.selectDeptList(new SysDept());
        // 先放入第一级父id
        ids.add(deptId);
        fillChildIds(all, ids, deptId); // 递归算法

        // 3. 存入 Redis，设置过期时间
        redisTemplate.opsForValue().set(key, ids, 24, TimeUnit.HOURS);

        return ids;
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