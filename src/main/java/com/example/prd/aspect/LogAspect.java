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
 * 拦截所有标注 {@link Log} 的方法：执行成功或抛异常后，组装日志并异步落库。
 * 日志功能的异常绝对不能影响正常业务逻辑。
 */
@Aspect   // 告诉 Spring：这是一个切面类，用来拦截其他代码
@Component // 交给 Spring 管理，否则切面不生效
public class LogAspect {

    @Autowired
    private SysOperLogService operLogService; // 注入日志服务，最后把记录存进数据库

    /**
     * 配置织入点：监听所有加了 {@link Log} 注解的方法。
     * 方法正常执行完 或 报错抛出异常 时，都会进入下方的通知方法写库。
     */
    @Pointcut("@annotation(com.example.prd.annotation.Log)")
    public void logPointCut() {
    }

    /**
     * 方法正常返回后执行
     *
     * @param joinPoint  当前被拦截的方法信息（类名、方法名、参数等）
     * @param jsonResult 方法返回值（如 Result 对象），由 Spring 自动传入
     */
    @AfterReturning(pointcut = "logPointCut()", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Object jsonResult) {
        handleLog(joinPoint, null, jsonResult);
    }

    /**
     * 方法抛出异常后执行
     */
    @AfterThrowing(value = "logPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }

    /**
     * 统一组装并持久化操作日志
     *
     * @param joinPoint  被拦截的方法信息
     * @param e          异常信息，无异常时为 null
     * @param jsonResult 方法返回值，异常时为 null
     */
    protected void handleLog(JoinPoint joinPoint, Exception e, Object jsonResult) {
        try {
            // ---------- 1. 从 Spring 请求上下文中取出 HttpServletRequest ----------
            // Spring 给每个请求维护了一个“小抽屉”（RequestContextHolder）
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();

            // ---------- 2. 通过 JoinPoint 拿到被拦截的方法，再读取 @Log 注解 ----------
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();           // 完整的方法对象
            Log log = method.getAnnotation(Log.class);       // 从方法上取出 @Log 注解
            if (log == null) {
                return;
            }

            // ---------- 3. 构建日志实体 ----------
            SysOperLog operLog = new SysOperLog();
            operLog.setStatus(0);                            // 默认 0 表示成功
            operLog.setOperTime(new Date());
            operLog.setOperUrl(request.getRequestURI());
            operLog.setOperUser("SYSTEM_USER");              // 演示用固定用户；生产可从 SecurityContext 取登录人

            if (e != null) {
                operLog.setStatus(1);                        // 1 表示失败
                operLog.setErrorMsg(e.getMessage());
            }

            // ---------- 4. 记录类名 + 方法名 ----------
            // 例如：com.example.prd.controller.PrdCheckListController.save()
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            operLog.setTitle(log.title());
            operLog.setBusinessType(log.businessType());

            // ---------- 5. 序列化请求参数与返回值 ----------
            try {
                // 5.1 把前端传入的参数转成 JSON 存进 oper_param
                operLog.setOperParam(JSON.toJSONString(joinPoint.getArgs()));
                if (jsonResult != null) {
                    // 5.2 若有返回值，也转成 JSON 存进 json_result
                    operLog.setJsonResult(JSON.toJSONString(jsonResult));
                }
            } catch (Exception jsonEx) {
                operLog.setOperParam("参数序列化失败");
            }

            // 切面只负责组装数据；真正写库在 Service，且带 @Async 异步执行，不阻塞主线程
            operLogService.insertOperLog(operLog);

        } catch (Exception ex) {
            // 日志模块自身出错时，只打印，不能拖垮业务接口
            ex.printStackTrace();
        }
    }
}
