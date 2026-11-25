package com.zhe.log.config;

import com.zhe.log.aspect.LogPrintAspect;
import com.zhe.log.aspect.RequestLogAspect;
import com.zhe.log.aspect.ResponseLogAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(ZheLogProperties.class)
@ConditionalOnProperty(prefix = "zhe.log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ZheLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LogPrintAspect logPrintAspect(ZheLogProperties properties) {
        return new LogPrintAspect(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestLogAspect requestLogAspect(ZheLogProperties properties) {
        return new RequestLogAspect(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ResponseLogAspect responseLogAspect(ZheLogProperties properties) {
        return new ResponseLogAspect(properties);
    }
}
