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

    // 获取整个部门树结构
    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts) {
        // 1. 先尝试从缓存拿
        List<SysDept> cachedTree = (List<SysDept>) redisTemplate.opsForValue().get(CACHE_KEY_TREE);
        if (cachedTree != null) {
            return cachedTree;
        }

        // 2. 缓存没有，准备加锁,定义锁的名字,所有请求都抢这一把锁。
        String lockKey = LOCK_KEY_PREFIX + "tree";
/*
抢锁，锁有效期 10 秒，防止死锁（锁永远留在 Redis 里 → 后面所有线程永远抢不到锁 → 整个功能卡死）
线程 A（第一个跑到的）
setIfAbsent(lockKey) → 锁不存在
→ 设置成功
→ isLock = true
→ 进入 if 里面
→ 去查库、构建树
→ 剩下的就只能等待
10s后锁过期了 → A业务还没跑完 → 锁被别人抢走 → 并发问题又来了
使用 Redisson 分布式锁，它自带看门狗机制，会自动给锁续期，只要线程没跑完，锁就不会过期，保证安全。
*/
        try {
            Boolean isLock = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(isLock)) {
                // 双重检查拿到锁后再次确认缓存，防止重复计算
                // 第一个抢到锁的线程，可能已经把缓存重建完了。后面抢到的线程就不要再构建了
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
            // A 线程执行完后释放锁
            redisTemplate.delete(lockKey);
        }
    }

    // 查询出当前部门下的子部门 返回的列表
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

                // 执行递归逻辑 列表
                List<Long> ids = new ArrayList<>();
                List<SysDept> all = deptMapper.selectDeptList(new SysDept());
                // 先把父部门id作为列表第一个
                ids.add(deptId);
                // 挨个遍历all 如果里面的元素的父id==当前deptId的，就放入结果列表，然后递归调用，放入的在作为父id
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
            // 凡是会让线程等待的方法，都必须捕获或抛出中断异常。
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