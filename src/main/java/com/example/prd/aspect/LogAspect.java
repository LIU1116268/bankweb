/**
 * @description: 银行投产材料管理系统 - 核心业务逻辑实现
 * @author: LLQ (Researcher @ SCU-CEIE)
 * @date: 2026-03
 * @version: 1.0
 * @note: 本项目仅用于个人技术方案复现与脱敏展示，严禁用于其他任何用途。
 */

package com.example.prd.aspect;

import com.alibaba.fastjson2.JSON;
import com.example.prd.annotation.Log;
import com.example.prd.entity.SysOperLog;
import com.example.prd.service.SysOperLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * 操作日志记录处理
 */
@Aspect
@Component
public class LogAspect {

    @Autowired
    private SysOperLogService operLogService;

    // 配置织入点：监听所有加了 @Log 注解的方法
    @Pointcut("@annotation(com.example.prd.annotation.Log)")
    public void logPointCut() {
    }

    /**
     * 处理完请求后执行（正常执行）
     */
    @AfterReturning(pointcut = "logPointCut()", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Object jsonResult) {
        handleLog(joinPoint, null, jsonResult);
    }

    /**
     * 拦截异常操作
     */
    @AfterThrowing(value = "logPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, final Exception e, Object jsonResult) {
        try {
            // 1. 获取当前请求属性
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;
            HttpServletRequest request = attributes.getRequest();

            // 2. 获取方法上的注解信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Log log = method.getAnnotation(Log.class);

            if (log == null) return;

            // 3. 构建日志实体
            SysOperLog operLog = new SysOperLog();
            operLog.setStatus(0); // 正常
            operLog.setOperTime(new Date());
            operLog.setOperUrl(request.getRequestURI());
            operLog.setOperUser("SYSTEM_USER"); // 实际可从 SecurityContext 获取

            // 记录异常信息
            if (e != null) {
                operLog.setStatus(1);
                operLog.setErrorMsg(e.getMessage());
            }

            // 4. 设置类名与方法名
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            operLog.setTitle(log.title());
            operLog.setBusinessType(log.businessType());

            // 5. 参数与结果转换（转为JSON字符串存入数据库）
            // 注意：这里需要 try-catch 防止 JSON 转换失败导致业务报错
            try {
                operLog.setOperParam(JSON.toJSONString(joinPoint.getArgs()));
                if (jsonResult != null) {
                    operLog.setJsonResult(JSON.toJSONString(jsonResult));
                }
            } catch (Exception jsonEx) {
                operLog.setOperParam("参数序列化失败");
            }

            // 6. 调用异步 Service 落库
            // 这体现了：切面只管组装数据，Service 只管存，异步不阻塞主线程
            operLogService.insertOperLog(operLog);

        } catch (Exception ex) {
            // 规范：日志功能的异常绝对不能影响正常业务逻辑
            ex.printStackTrace();
        }
    }
}