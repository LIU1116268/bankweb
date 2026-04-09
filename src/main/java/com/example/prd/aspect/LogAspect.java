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
@Aspect // 告诉 Spring：这是一个切面类，用来拦截其他代码的
@Component // 丢给 Spring 管理，否则它不生效
public class LogAspect {
    @Autowired
    private SysOperLogService operLogService; // 注入日志服务，最后要把笔记存进数据库

    // 配置织入点：监听所有加了 @Log 注解的方法，
    // 只要加了 @Log 的方法，执行完 或 报错，就自动记录日志到数据库。
    // 所以我这个切片类的名字可以随便叫，因为这设置他只跟Log关联
    @Pointcut("@annotation(com.example.prd.annotation.Log)")
    public void logPointCut() {}

    /**
     * 处理完请求后执行（正常执行）
     */
    @AfterReturning(
            pointcut = "logPointCut()",  // 切点：哪些方法需要拦截
            returning = "jsonResult"  // 把方法返回的结果，取名叫jsonResult
    )
    public void doAfterReturning(JoinPoint joinPoint, // 代表当前被拦截的方法信息
                                 Object jsonResult ){// 方法返回的值jsonResult（比如返回列表、返回对象）
            handleLog(joinPoint,null,jsonResult);
        }
    /**
     * 拦截异常操作
     */
    @AfterThrowing(value = "logPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, // 入参1：当前被拦截的方法信息
                             final Exception e, // 入参2：异常信息（无异常则为null）
                             Object jsonResult)  // 入参3：方法返回值（异常时为null）
    {
        try {
            // 1. Spring 给每个请求开了一个小抽屉，这里面放着 request。请求上下文
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;
            // 从刚才拿到的 “请求抽屉” 里，把真正的请求对象取出来方进request。请求对象
            HttpServletRequest request = attributes.getRequest();

            // 2. 通过joinPoint获取方法，再从方法上获得注解信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();//完整的方法对象，整个save内容
            Log log = method.getAnnotation(Log.class);// 把贴在上面的 @Log 注解撕下来

            if (log == null) return;

            // 3. 构建日志实体
            SysOperLog operLog = new SysOperLog();
            operLog.setStatus(0); // 正常
            operLog.setOperTime(new Date());
            operLog.setOperUrl(request.getRequestURI());
            operLog.setOperUser("SYSTEM_USER"); // 实际可从 SecurityContext 获取

            if (e != null) {
                operLog.setStatus(1);// 出现异常，就又把状态置1
                operLog.setErrorMsg(e.getMessage());
            }
            // 4. 设置类名与方法名
            // com.example.prd.controller.CheckListController  类名
            String className = joinPoint.getTarget().getClass().getName();
            // save 方法名
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            operLog.setTitle(log.title());
            operLog.setBusinessType(log.businessType());

            // 5. 获取请求的参数，并设置到操作日志中
            try {
                // 5.1. 把前端传的参数转成 JSON字符串，存进日志
                operLog.setOperParam(JSON.toJSONString(joinPoint.getArgs()));
                if (jsonResult != null) {
                    // 5.2. 如果接口有返回值，也转成 JSON字符串，存进日志
                    operLog.setJsonResult(JSON.toJSONString(jsonResult));
                }
            } catch (Exception jsonEx) {
                operLog.setOperParam("参数序列化失败");
            }


            // 切面只管组装数据，Service 只管存，异步不阻塞主线程
            operLogService.insertOperLog(operLog);

        } catch (Exception ex) {
            // 日志功能的异常绝对不能影响正常业务逻辑
            ex.printStackTrace();
        }
    }
}