package com.zhe.log.aspect;

import com.zhe.log.annotation.LogPrint;
import com.zhe.log.context.LogContext;
import com.zhe.log.config.ZheLogProperties;
import com.zhe.log.util.JsonUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;

/**
 * {@link LogPrint} 切面：入口打印参数，最外层调用返回/抛出时打印结果或异常与总耗时。
 * 通过 ThreadLocal 记录调用深度，finally 中恢复/清理，避免线程池场景下的状态泄漏。
 */
@Aspect
public class LogPrintAspect implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(LogPrintAspect.class);

    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<>();

    private final ZheLogProperties properties;

    public LogPrintAspect(ZheLogProperties properties) {
        this.properties = properties;
    }

    @Override
    public int getOrder() {
        return properties.getLogPrintOrder();
    }

    @Pointcut("@annotation(com.zhe.log.annotation.LogPrint) || @within(com.zhe.log.annotation.LogPrint)")
    public void logPrintPointcut() {
    }

    @Around("logPrintPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Integer depth = DEPTH.get();
        boolean outermost = depth == null;
        DEPTH.set(outermost ? 1 : depth + 1);
        long start = System.currentTimeMillis();
        try {
            logParams(joinPoint);
            Object result = joinPoint.proceed();
            if (outermost) {
                logResult(joinPoint, result, System.currentTimeMillis() - start);
            }
            return result;
        } catch (Throwable cause) {
            if (outermost) {
                logThrow(joinPoint, cause, System.currentTimeMillis() - start);
            }
            throw cause;
        } finally {
            if (outermost) {
                DEPTH.remove();
            } else {
                DEPTH.set(depth);
            }
        }
    }

    private void logParams(ProceedingJoinPoint joinPoint) {
        try {
            Class<?> clazz = joinPoint.getTarget().getClass();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String params = JsonUtil.argsToString(signature.getParameterNames(), joinPoint.getArgs());
            log.info("user={} {}#{}() param={}", LogContext.getUserId(), clazz.getName(), method.getName(), params);
        } catch (Exception e) {
            log.warn("user={} @LogPrint logParams error. e=", LogContext.getUserId(), e);
        }
    }

    private void logResult(ProceedingJoinPoint joinPoint, Object result, long costMillis) {
        try {
            Class<?> clazz = joinPoint.getTarget().getClass();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            log.info("user={} {}#{}() result={}, cost: {}ms",
                    LogContext.getUserId(), clazz.getName(), method.getName(), JsonUtil.toLogString(result), costMillis);
        } catch (Exception e) {
            log.warn("user={} @LogPrint logResult error. e=", LogContext.getUserId(), e);
        }
    }

    private void logThrow(ProceedingJoinPoint joinPoint, Throwable cause, long costMillis) {
        try {
            Class<?> clazz = joinPoint.getTarget().getClass();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            // 尾部再传一次 cause 让 SLF4J 附带完整堆栈，占位符用 toString 保证消息本体可检索
            log.error("user={} {}#{}() throw={}, cost: {}ms",
                    LogContext.getUserId(), clazz.getName(), method.getName(), cause.toString(), costMillis, cause);
        } catch (Exception e) {
            log.warn("user={} @LogPrint logThrow error. e=", LogContext.getUserId(), e);
        }
    }
}
