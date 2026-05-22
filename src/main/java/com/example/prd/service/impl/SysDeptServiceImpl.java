package com.example.prd.service.impl;

import com.example.prd.entity.SysDept;
import com.example.prd.mapper.SysDeptMapper;
import com.example.prd.service.SysDeptService;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 组织架构（部门）- 业务实现
 * <p>
 * 缓存策略：旁路缓存（Cache Aside）+ 变更时删除缓存 + 查询时延迟重建
 * <p>
 * 分布式锁：Redisson 看门狗自动续期，避免原生 setIfAbsent 锁过期导致并发安全问题
 */
@Service
public class SysDeptServiceImpl implements SysDeptService {

    private static final String CACHE_KEY_TREE = "bankweb:dept:tree";
    private static final String CACHE_KEY_CHILDREN = "bankweb:dept:childrenIds:";
    private static final String LOCK_KEY_PREFIX = "bankweb:lock:";
    private static final long CACHE_TTL_HOURS = 24L;
    private static final long LOCK_RETRY_SLEEP_MS = 50L;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    // ==================== 查询 ====================

    @Override
    public List<SysDept> selectDeptList(SysDept dept) {
        return deptMapper.selectDeptList(dept);
    }

    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts) {
        List<SysDept> cachedTree = getTreeFromCache();
        if (cachedTree != null) {
            return cachedTree;
        }

        String lockKey = LOCK_KEY_PREFIX + "tree";
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(0, TimeUnit.SECONDS)) {
                cachedTree = getTreeFromCache();
                if (cachedTree != null) {
                    return cachedTree;
                }
                List<SysDept> tree = buildTreeFromFlatList(depts);
                redisTemplate.opsForValue().set(CACHE_KEY_TREE, tree, CACHE_TTL_HOURS, TimeUnit.HOURS);
                return tree;
            }
            Thread.sleep(LOCK_RETRY_SLEEP_MS);
            return buildDeptTree(depts);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public List<Long> selectChildrenIds(Long deptId) {
        String cacheKey = CACHE_KEY_CHILDREN + deptId;
        List<Long> cachedIds = getChildrenIdsFromCache(cacheKey);
        if (cachedIds != null) {
            return cachedIds;
        }

        String lockKey = LOCK_KEY_PREFIX + "childrenIds:" + deptId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(0, TimeUnit.SECONDS)) {
                cachedIds = getChildrenIdsFromCache(cacheKey);
                if (cachedIds != null) {
                    return cachedIds;
                }
                List<Long> ids = computeChildrenIds(deptId);
                redisTemplate.opsForValue().set(cacheKey, ids, CACHE_TTL_HOURS, TimeUnit.HOURS);
                return ids;
            }
            Thread.sleep(LOCK_RETRY_SLEEP_MS);
            return selectChildrenIds(deptId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ==================== 维护 ====================

    @Override
    @Transactional
    public int insertDept(SysDept dept) {
        int rows = deptMapper.insert(dept);
        if (rows > 0) {
            clearDeptCache();
        }
        return rows;
    }

    @Override
    public void clearDeptCache() {
        redisTemplate.delete(CACHE_KEY_TREE);
        ScanOptions options = ScanOptions.scanOptions()
                .match(CACHE_KEY_CHILDREN + "*")
                .count(1000)
                .build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            List<String> keysToDelete = new ArrayList<>();
            while (cursor.hasNext()) {
                keysToDelete.add(cursor.next());
            }
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
            }
        }
    }

    // ==================== 私有方法 ====================

    @SuppressWarnings("unchecked")
    private List<SysDept> getTreeFromCache() {
        return (List<SysDept>) redisTemplate.opsForValue().get(CACHE_KEY_TREE);
    }

    @SuppressWarnings("unchecked")
    private List<Long> getChildrenIdsFromCache(String cacheKey) {
        return (List<Long>) redisTemplate.opsForValue().get(cacheKey);
    }

    private List<SysDept> buildTreeFromFlatList(List<SysDept> depts) {
        return depts.stream()
                .filter(d -> d.getParentId() == 0)
                .map(d -> {
                    d.setChildren(getChildren(d, depts));
                    return d;
                })
                .collect(Collectors.toList());
    }

    private List<Long> computeChildrenIds(Long deptId) {
        List<Long> ids = new ArrayList<>();
        List<SysDept> all = deptMapper.selectDeptList(new SysDept());
        ids.add(deptId);
        fillChildIds(all, ids, deptId);
        return ids;
    }

    private List<SysDept> getChildren(SysDept parent, List<SysDept> all) {
        return all.stream()
                .filter(d -> d.getParentId().equals(parent.getDeptId()))
                .map(d -> {
                    d.setChildren(getChildren(d, all));
                    return d;
                })
                .collect(Collectors.toList());
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
