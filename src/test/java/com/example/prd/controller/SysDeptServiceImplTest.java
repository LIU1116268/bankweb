package com.example.prd.controller;


import com.example.prd.entity.SysDept;
import com.example.prd.mapper.SysDeptMapper;
import com.example.prd.service.impl.SysDeptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SysDeptServiceImplTest {

    @Mock
    private SysDeptMapper deptMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private SysDeptServiceImpl sysDeptService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 模拟 redisTemplate.opsForValue() 的返回
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("测试递归查询子部门ID集合（不走缓存情况）")
    void testSelectChildrenIdsNoCache() {
        // 1. 准备模拟数据：总行(100) -> 分行(101) -> 支行(103)
        Long targetId = 100L;
        List<SysDept> allDepts = new ArrayList<>();
        allDepts.add(createDept(100L, 0L, "总行"));
        allDepts.add(createDept(101L, 100L, "成都分行"));
        allDepts.add(createDept(103L, 101L, "武侯支行"));
        allDepts.add(createDept(102L, 0L, "其他无关部门"));

        // 2. 模拟行为
        when(valueOperations.get(anyString())).thenReturn(null); // 模拟缓存未命中
        when(deptMapper.selectDeptList(any())).thenReturn(allDepts);

        // 3. 执行测试
        List<Long> resultIds = sysDeptService.selectChildrenIds(targetId);

        // 4. 断言验证
        assertNotNull(resultIds);
        assertEquals(3, resultIds.size());
        assertTrue(resultIds.containsAll(Arrays.asList(100L, 101L, 103L)));
        assertFalse(resultIds.contains(102L));

        // 验证是否存入了缓存
        verify(valueOperations, times(1)).set(contains("childrenIds:"), any(), anyLong(), any());
    }

    @Test
    @DisplayName("测试递归构建部门树逻辑")
    void testBuildDeptTree() {
        // 1. 准备打平的部门列表
        List<SysDept> depts = Arrays.asList(
                createDept(100L, 0L, "总行"),
                createDept(101L, 100L, "分行")
        );

        when(valueOperations.get(anyString())).thenReturn(null); // 模拟缓存未命中

        // 2. 执行测试
        List<SysDept> tree = sysDeptService.buildDeptTree(depts);

        // 3. 断言
        assertEquals(1, tree.size()); // 顶级节点只有总行
        assertEquals("总行", tree.get(0).getDeptName());
        assertEquals(1, tree.get(0).getChildren().size()); // 总行下有一个孩子
        assertEquals("分行", tree.get(0).getChildren().get(0).getDeptName());
    }

    @Test
    @DisplayName("测试插入部门后自动清理缓存")
    void testInsertDeptAndClearCache() {
        SysDept newDept = createDept(200L, 100L, "新支行");
        when(deptMapper.insert(any())).thenReturn(1);

        // 执行插入
        sysDeptService.insertDept(newDept);

        // 验证是否调用了删除缓存的方法
        // 由于 clearDeptCache 内部会调用 redisTemplate.delete
        verify(redisTemplate, atLeastOnce()).delete(anyString());
    }

    // 快捷创建对象的方法
    private SysDept createDept(Long id, Long parentId, String name) {
        SysDept d = new SysDept();
        d.setDeptId(id);
        d.setParentId(parentId);
        d.setDeptName(name);
        return d;
    }
}