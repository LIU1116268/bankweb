package com.example.prd.aspect;

import com.alibaba.fastjson2.JSON;
import com.example.prd.annotation.Log;
import com.example.prd.entity.SysOperLog;
import com.example.prd.service.SysOperLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * 操作审计日志切面
 * <p>
 * 拦截所有标注 {@link Log} 的方法：正常返回或抛异常后，组装日志并异步落库
 */
@Aspect
@Component
public class LogAspect {

    @Autowired
    private SysOperLogService operLogService;

    @Pointcut("@annotation(com.example.prd.annotation.Log)")
    public void logPointCut() {
    }

    @AfterReturning(pointcut = "logPointCut()", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Object jsonResult) {
        handleLog(joinPoint, null, jsonResult);
    }

    @AfterThrowing(value = "logPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }

    /**
     * 组装并持久化操作日志（日志模块异常不得影响主业务）
     */
    protected void handleLog(JoinPoint joinPoint, Exception e, Object jsonResult) {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Log log = method.getAnnotation(Log.class);
            if (log == null) {
                return;
            }

            SysOperLog operLog = new SysOperLog();
            operLog.setStatus(e == null ? 0 : 1);
            operLog.setOperTime(new Date());
            operLog.setOperUrl(request.getRequestURI());
            operLog.setOperUser("SYSTEM_USER");
            if (e != null) {
                operLog.setErrorMsg(e.getMessage());
            }

            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            operLog.setTitle(log.title());
            operLog.setBusinessType(log.businessType());

            try {
                operLog.setOperParam(JSON.toJSONString(joinPoint.getArgs()));
                if (jsonResult != null) {
                    operLog.setJsonResult(JSON.toJSONString(jsonResult));
                }
            } catch (Exception ignored) {
                operLog.setOperParam("参数序列化失败");
            }

            operLogService.insertOperLog(operLog);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
