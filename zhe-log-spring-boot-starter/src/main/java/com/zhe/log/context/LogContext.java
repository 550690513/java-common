package com.zhe.log.context;

/**
 * 日志上下文：宿主应用在认证拦截器/过滤器等位置调用 {@link #setUserId}，
 * 日志切面即会在每条日志前缀中带上 user=xxx；未设置时打印 user=null。
 */
public final class LogContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private LogContext() {
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
