package com.zhe.log.aspect;

import com.zhe.log.annotation.ResponseLog;
import com.zhe.log.config.ZheLogProperties;
import com.zhe.log.context.LogContext;
import com.zhe.log.util.JsonUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;

/**
 * {@link ResponseLog} 切面：方法正常返回后打印返回值。
 */
@Aspect
public class ResponseLogAspect implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(ResponseLogAspect.class);

    private final ZheLogProperties properties;

    public ResponseLogAspect(ZheLogProperties properties) {
        this.properties = properties;
    }

    @Override
    public int getOrder() {
        return properties.getResponseLogOrder();
    }

    @Pointcut("@annotation(com.zhe.log.annotation.ResponseLog) || @within(com.zhe.log.annotation.ResponseLog)")
    public void responseLogPointcut() {
    }

    @AfterReturning(value = "responseLogPointcut()", returning = "res")
    public void afterEnhance(JoinPoint joinPoint, Object res) {
        try {
            Class<?> clazz = joinPoint.getTarget().getClass();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            log.info("user={} {}#{}() result={}",
                    LogContext.getUserId(), clazz.getName(), method.getName(), JsonUtil.toLogString(res));
        } catch (Exception e) {
            log.warn("user={} @ResponseLog afterEnhance error. e=", LogContext.getUserId(), e);
        }
    }
}
