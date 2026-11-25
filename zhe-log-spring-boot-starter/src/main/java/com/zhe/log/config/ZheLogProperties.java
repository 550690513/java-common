package com.zhe.log.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

/**
 * zhe.log 前缀配置项。order 用于与宿主应用的登录/权限等切面排序，
 * 数值越小越靠外层，默认 {@link Ordered#LOWEST_PRECEDENCE}（最靠近目标方法）。
 */
@ConfigurationProperties(prefix = "zhe.log")
public class ZheLogProperties {

    /**
     * 是否启用，可通过 zhe.log.enabled=false 整体关闭
     */
    private boolean enabled = true;

    private Integer logPrintOrder = Ordered.LOWEST_PRECEDENCE;

    private Integer requestLogOrder = Ordered.LOWEST_PRECEDENCE;

    private Integer responseLogOrder = Ordered.LOWEST_PRECEDENCE;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getLogPrintOrder() {
        return logPrintOrder;
    }

    public void setLogPrintOrder(Integer logPrintOrder) {
        this.logPrintOrder = logPrintOrder;
    }

    public Integer getRequestLogOrder() {
        return requestLogOrder;
    }

    public void setRequestLogOrder(Integer requestLogOrder) {
        this.requestLogOrder = requestLogOrder;
    }

    public Integer getResponseLogOrder() {
        return responseLogOrder;
    }

    public void setResponseLogOrder(Integer responseLogOrder) {
        this.responseLogOrder = responseLogOrder;
    }
}
