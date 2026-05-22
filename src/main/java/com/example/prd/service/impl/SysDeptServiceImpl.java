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
 * 缓存策略：旁路缓存（Cache Aside）+ 数据变更时删除缓存 + 查询时延迟重建。
 * <p>
 * -------------------------------------------------------------------------
 * 为什么用 Redisson 分布式锁，而不是原生 Redis setIfAbsent？
 * -------------------------------------------------------------------------
 * 原生 Redis 锁的问题：当前线程抢到的锁如果过期了，但业务还没执行完，
 * 其他线程就能拿到锁，造成缓存击穿、重复查库等并发安全问题。
 * <p>
 * Redisson 看门狗（WatchDog）机制：只要持有锁的线程还在运行，锁会自动续期，
 * 业务没跑完，锁就不会过期。
 * <p>
 * Redisson 还能解决：
 * <ol>
 *   <li>锁超时提前释放 —— 看门狗自动续期</li>
 *   <li>死锁 —— 服务宕机/线程异常终止后，看门狗线程销毁，锁到期自动释放</li>
 *   <li>误删别人的锁 —— 锁内记录线程标识，只能自己 unlock</li>
 *   <li>功能单一 —— 支持可重入锁、公平锁、读写锁等</li>
 * </ol>
 */
@Service
public class SysDeptServiceImpl implements SysDeptService {

    /** 整棵部门树的缓存 Key */
    private static final String CACHE_KEY_TREE = "bankweb:dept:tree";

    /** 某部门下全部子部门 ID 列表的缓存 Key 前缀，完整 Key = 前缀 + deptId */
    private static final String CACHE_KEY_CHILDREN = "bankweb:dept:childrenIds:";

    /** 分布式锁 Key 前缀 */
    private static final String LOCK_KEY_PREFIX = "bankweb:lock:";

    private static final long CACHE_TTL_HOURS = 24L;

    /** 没抢到锁时，休眠多久再自旋重试（毫秒） */
    private static final long LOCK_RETRY_SLEEP_MS = 50L;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /** Redisson 客户端：用于分布式锁 RLock、与限流模块共用同一 Redis 连接 */
    @Resource
    private RedissonClient redissonClient;

    // ==================== 查询 ====================

    @Override
    public List<SysDept> selectDeptList(SysDept dept) {
        return deptMapper.selectDeptList(dept);
    }

    /**
     * 构建部门树（带 Redis 缓存 + Redisson 分布式锁防击穿）
     */
    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts) {
        // 1. 先查缓存，命中则直接返回
        List<SysDept> cachedTree = getTreeFromCache();
        if (cachedTree != null) {
            return cachedTree;
        }

        String lockKey = LOCK_KEY_PREFIX + "tree";
        // 拿到分布式锁（内部已自动开启看门狗续期）
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 2. 尝试加锁：waitTime=0 表示不排队，抢不到立刻返回 false
            //    只传 (0, TimeUnit.SECONDS) 时，Redisson 会启用看门狗，锁不会因固定 TTL 提前过期
            // 常见写法对比（面试可讲）：
            //   tryLock(0, 30, SECONDS)  → 不启用看门狗，最多锁 30 秒
            //   tryLock(0, TimeUnit.SECONDS) → 启用看门狗，业务没完锁不过期
            // 源码内部 tryLockAsync(waitTime, -1, ...) 的 -1 表示看门狗；
            // 对外 API 若写 -1 会被转换成 -1000ms，所以不要手写 -1
            boolean isLock = lock.tryLock(0, TimeUnit.SECONDS);

            if (isLock) {
                // 3. 双重检查：拿到锁后再看一遍缓存，可能已被其他线程建好
                cachedTree = getTreeFromCache();
                if (cachedTree != null) {
                    return cachedTree;
                }

                // 4. 真正查库并递归组装树
                List<SysDept> tree = buildTreeFromFlatList(depts);

                // 5. 写入缓存，过期 24 小时
                redisTemplate.opsForValue().set(CACHE_KEY_TREE, tree, CACHE_TTL_HOURS, TimeUnit.HOURS);
                return tree;
            } else {
                // 6. 没抢到锁：短暂休眠后递归重试（自旋），直到拿到锁并返回缓存/新树
                Thread.sleep(LOCK_RETRY_SLEEP_MS);
                return buildDeptTree(depts);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } finally {
            // 只释放当前线程持有的锁，防止误删其他线程的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 查询某部门及其全部下级部门 ID（带缓存 + 按 deptId 独立加锁）
     */
    @Override
    public List<Long> selectChildrenIds(Long deptId) {
        String cacheKey = CACHE_KEY_CHILDREN + deptId;

        // 1. 先查缓存
        List<Long> cachedIds = getChildrenIdsFromCache(cacheKey);
        if (cachedIds != null) {
            return cachedIds;
        }

        // 2. 每个 deptId 使用一把独立锁，避免不同部门之间互相阻塞
        String lockKey = LOCK_KEY_PREFIX + "childrenIds:" + deptId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLock = lock.tryLock(0, TimeUnit.SECONDS);

            if (isLock) {
                // 双重检查
                cachedIds = getChildrenIdsFromCache(cacheKey);
                if (cachedIds != null) {
                    return cachedIds;
                }

                // 递归收集本部门 + 全部子孙部门 ID
                List<Long> ids = computeChildrenIds(deptId);

                // 写入缓存
                redisTemplate.opsForValue().set(cacheKey, ids, CACHE_TTL_HOURS, TimeUnit.HOURS);
                return ids;
            } else {
                // 自旋重试
                Thread.sleep(LOCK_RETRY_SLEEP_MS);
                return selectChildrenIds(deptId);
            }
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

    /**
     * 清理部门相关 Redis 缓存
     * <p>
     * 采用【删缓存】而非【直接改缓存】的原因：
     * 部门树是递归组装的复杂结构，在缓存里局部改节点容易出错；
     * 删缓存后下次查询再从库中全量重建，实现简单且能保证强一致。
     */
    @Override
    public void clearDeptCache() {
        // 删除整棵树缓存
        redisTemplate.delete(CACHE_KEY_TREE);

        // 使用 SCAN 分批匹配 childrenIds:*，避免 KEYS 阻塞 Redis 主线程
        ScanOptions options = ScanOptions.scanOptions()
                .match(CACHE_KEY_CHILDREN + "*")  // 匹配 bankweb:dept:childrenIds: 开头的 Key
                .count(1000)
                .build();

        // Cursor 像书签，记录扫描进度，不会一次性拉取全部 Key
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

    /** 将扁平部门列表组装为树：先取 parentId=0 的根节点，再递归挂子节点 */
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

    /** DFS 递归收集所有子孙部门 ID */
    private void fillChildIds(List<SysDept> all, List<Long> ids, Long parentId) {
        for (SysDept d : all) {
            if (d.getParentId().equals(parentId)) {
                ids.add(d.getDeptId());
                fillChildIds(all, ids, d.getDeptId());
            }
        }
    }
}
