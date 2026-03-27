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
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
public class SysDeptServiceImpl implements SysDeptService {

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_TREE = "bankweb:dept:tree";
    private static final String CACHE_KEY_CHILDREN = "bankweb:dept:childrenIds:";
    // 定义锁的前缀
    private static final String LOCK_KEY_PREFIX = "bankweb:lock:";

    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts) {
        // 1. 先尝试从缓存拿
        List<SysDept> cachedTree = (List<SysDept>) redisTemplate.opsForValue().get(CACHE_KEY_TREE);
        if (cachedTree != null) {
            return cachedTree;
        }

        // 2. 缓存没有，准备加锁
        String lockKey = LOCK_KEY_PREFIX + "tree";
        try {
            // 尝试加锁，有效期 10 秒，防止死锁
            Boolean isLock = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(isLock)) {
                // 【关键：双重检查】拿到锁后再次确认缓存，防止重复计算
                cachedTree = (List<SysDept>) redisTemplate.opsForValue().get(CACHE_KEY_TREE);
                if (cachedTree != null) return cachedTree;

                // 执行耗时的递归构建逻辑
                List<SysDept> tree = depts.stream()
                        .filter(d -> d.getParentId() == 0)
                        .map(d -> {
                            d.setChildren(getChildren(d, depts));
                            return d;
                        }).collect(Collectors.toList());

                // 写入缓存
                redisTemplate.opsForValue().set(CACHE_KEY_TREE, tree, 24, TimeUnit.HOURS);
                return tree;
            } else {
                // 没拿到锁的线程：休眠一会儿再重试（自旋）
                Thread.sleep(100);
                return buildDeptTree(depts);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } finally {
            // 释放锁
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public List<Long> selectChildrenIds(Long deptId) {
        String cacheKey = CACHE_KEY_CHILDREN + deptId;

        // 1. 先查缓存
        List<Long> cachedIds = (List<Long>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedIds != null) return cachedIds;

        // 2. 加分布式锁（每个部门 ID 对应一把独立的锁）
        String lockKey = LOCK_KEY_PREFIX + "childrenIds:" + deptId;
        try {
            Boolean isLock = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(isLock)) {
                // 双重检查
                cachedIds = (List<Long>) redisTemplate.opsForValue().get(cacheKey);
                if (cachedIds != null) return cachedIds;

                // 执行计算逻辑
                List<Long> ids = new ArrayList<>();
                List<SysDept> all = deptMapper.selectDeptList(new SysDept());
                ids.add(deptId);
                fillChildIds(all, ids, deptId);

                // 存入缓存
                redisTemplate.opsForValue().set(cacheKey, ids, 24, TimeUnit.HOURS);
                return ids;
            } else {
                // 没抢到锁，等待重试
                Thread.sleep(50);
                return selectChildrenIds(deptId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    // --- 辅助方法---

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

    @Override
    public void clearDeptCache() {
        redisTemplate.delete(CACHE_KEY_TREE);
        java.util.Set<String> keys = redisTemplate.keys(CACHE_KEY_CHILDREN + "*");
        if (keys != null) redisTemplate.delete(keys);
    }

    @Override
    @Transactional
    public int insertDept(SysDept dept) {
        int rows = deptMapper.insert(dept);
        if (rows > 0) clearDeptCache();
        return rows;
    }

    @Override
    public List<SysDept> selectDeptList(SysDept dept) {
        return deptMapper.selectDeptList(dept);
    }
}