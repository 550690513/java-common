package com.zhe.log.aspect;

import com.zhe.log.annotation.RequestLog;
import com.zhe.log.config.ZheLogProperties;
import com.zhe.log.context.LogContext;
import com.zhe.log.util.JsonUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;

/**
 * {@link RequestLog} 切面：方法执行前打印入参，抛出异常时打印异常信息。
 */
@Aspect
public class RequestLogAspect implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLogAspect.class);

    private final ZheLogProperties properties;

    public RequestLogAspect(ZheLogProperties properties) {
        this.properties = properties;
    }

    @Override
    public int getOrder() {
        return properties.getRequestLogOrder();
    }

    @Pointcut("@annotation(com.zhe.log.annotation.RequestLog) || @within(com.zhe.log.annotation.RequestLog)")
    public void requestLogPointcut() {
    }

    @Before("requestLogPointcut()")
    public void beforeEnhance(JoinPoint joinPoint) {
        try {
            Class<?> clazz = joinPoint.getTarget().getClass();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String params = JsonUtil.argsToString(signature.getParameterNames(), joinPoint.getArgs());
            log.info("user={} {}#{}() param={}", LogContext.getUserId(), clazz.getName(), method.getName(), params);
        } catch (Exception e) {
            log.warn("user={} @RequestLog beforeEnhance error. e=", LogContext.getUserId(), e);
        }
    }

    @AfterThrowing(value = "requestLogPointcut()", throwing = "cause")
    public void afterThrowEnhance(JoinPoint joinPoint, Throwable cause) {
        try {
            Class<?> clazz = joinPoint.getTarget().getClass();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            // 尾部再传一次 cause 让 SLF4J 附带完整堆栈，占位符用 toString 保证消息本体可检索
            log.info("user={} {}#{}() throw={}",
                    LogContext.getUserId(), clazz.getName(), method.getName(), cause.toString(), cause);
        } catch (Exception e) {
            log.warn("user={} @RequestLog afterThrowEnhance error. e=", LogContext.getUserId(), e);
        }
    }
}
