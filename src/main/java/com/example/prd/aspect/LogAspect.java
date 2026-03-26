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

    // 配置织入点：监听所有加了 @Log 注解的方法
    // 监控范围
    @Pointcut("@annotation(com.example.prd.annotation.Log)")
    public void logPointCut() {
    }

    /**
     * 处理完请求后执行（正常执行）
     */
    @AfterReturning(
            pointcut = "logPointCut()",  // 切点：哪些方法需要拦截
            returning = "jsonResult"  // 把方法返回值绑定到变量 jsonResult
    )
    public void doAfterReturning(JoinPoint joinPoint, // 代表当前被拦截的方法信息
                                 Object jsonResult // 统一处理日志
    ) {
        handleLog(joinPoint,//有连接点信息
                null,
                jsonResult //有返回结果
        );
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
/*
通过 Spring 工具类(RequestContextHolder)，强行拿到当前的 HTTP 请求。
这样才能知道是哪个 IP、哪个 URL（比如 /add）在操作
RequestContextHolder.getRequestAttributes()
↓（返回父类：RequestAttributes）
(ServletRequestAttributes)  ← 强转！
          ↓
attributes = {
    request: HttpServletRequest  ← 里面装着完整请求信息
    response: HttpServletResponse
}
          ↓
attributes.getRequest()
          ↓
request = {
    url: "/user/add",
    ip: "192.168.1.100",
    method: "POST",
    header: "Bearer xxxx",
    parameter: { "id":1, "name":"张三" }
}
             */
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;
            HttpServletRequest request = attributes.getRequest();

            // 2. 通过joinPoint获取方法上的注解信息
            // 获取这个方法的签名即签名 = 方法的基本信息摘要
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();//【完整的方法对象】，整个save内容
            Log log = method.getAnnotation(Log.class);// 把贴在上面的 @Log 注解撕下来

            if (log == null) return;

            // 3. 构建日志实体
            SysOperLog operLog = new SysOperLog();
            operLog.setStatus(0); // 正常
            operLog.setOperTime(new Date());
            operLog.setOperUrl(request.getRequestURI());
            operLog.setOperUser("SYSTEM_USER"); // 实际可从 SecurityContext 获取
            // 记录异常信息
            if (e != null) {
                operLog.setStatus(1);// 出现异常，就又把状态置1
                operLog.setErrorMsg(e.getMessage());
            }
            // 4. 设置类名与方法名
            // com.example.prd.controller.CheckListController
            String className = joinPoint.getTarget().getClass().getName();
            // save
            String methodName = joinPoint.getSignature().getName();
            // Method=com.example.prd.controller.PrdCheckListController.save()
            operLog.setMethod(className + "." + methodName + "()");
            operLog.setTitle(log.title());
            operLog.setBusinessType(log.businessType());

            // 5. 比如新增传入的json 还有执行完得到的json也放入实体
            // 注意：这里需要 try-catch 防止 JSON 转换失败导致业务报错
            try {
                // 1. 把【前端传的参数】转成 JSON字符串，存进日志
                operLog.setOperParam(JSON.toJSONString(joinPoint.getArgs()));
                if (jsonResult != null) {
                    // 2. 如果【接口有返回值】，也转成 JSON字符串，存进日志
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