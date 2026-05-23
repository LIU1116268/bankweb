package com.example.prd.interceptor;

import com.example.prd.context.DataScopeContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 解析请求头中的机构信息，写入 {@link DataScopeContext}
 * <p>
 * Apifox 调试：在「Headers」里加 {@code X-Dept-Id: 101} 模拟四川省分行用户
 */
@Component
public class DataScopeInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String deptHeader = request.getHeader(DataScopeContext.HEADER_DEPT_ID);
        if (StringUtils.hasText(deptHeader)) {
            try {
                DataScopeContext.setDeptId(Long.parseLong(deptHeader.trim()));
            } catch (NumberFormatException ignored) {
                // 非法 header 视为未启用数据权限
            }
        }
        String userHeader = request.getHeader(DataScopeContext.HEADER_USER_ID);
        if (StringUtils.hasText(userHeader)) {
            DataScopeContext.setUserId(userHeader.trim());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 必须清理，避免 Tomcat 线程池复用导致串数据
        DataScopeContext.clear();
    }
}
