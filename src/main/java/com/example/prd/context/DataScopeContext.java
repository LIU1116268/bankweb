package com.example.prd.context;

/**
 * 机构数据权限上下文（ThreadLocal）
 * <p>
 * 演示环境通过请求头 {@code X-Dept-Id} 模拟「当前登录用户所属机构」。
 * 生产环境可改为从 JWT / SSO 解析后写入。
 */
public final class DataScopeContext {

    /** 请求头：当前用户所属部门 ID */
    public static final String HEADER_DEPT_ID = "X-Dept-Id";

    /** 请求头：当前用户工号（可选，便于审计） */
    public static final String HEADER_USER_ID = "X-User-Id";

    private static final ThreadLocal<Long> DEPT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    private DataScopeContext() {
    }

    public static void setDeptId(Long deptId) {
        DEPT_ID.set(deptId);
    }

    public static Long getDeptId() {
        return DEPT_ID.get();
    }

    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    /** 是否已启用机构数据权限（带了 X-Dept-Id 即为启用） */
    public static boolean isEnabled() {
        return getDeptId() != null;
    }

    public static void clear() {
        DEPT_ID.remove();
        USER_ID.remove();
    }
}
