package com.zhe.log.config;

import com.zhe.log.aspect.LogPrintAspect;
import com.zhe.log.aspect.RequestLogAspect;
import com.zhe.log.aspect.ResponseLogAspect;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;

class ZheLogAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ZheLogAutoConfiguration.class));

    @Test
    void registersAllAspectsByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(LogPrintAspect.class);
            assertThat(context).hasSingleBean(RequestLogAspect.class);
            assertThat(context).hasSingleBean(ResponseLogAspect.class);
            LogPrintAspect logPrintAspect = context.getBean(LogPrintAspect.class);
            assertThat(logPrintAspect.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
        });
    }

    @Test
    void disabledWhenZheLogEnabledFalse() {
        runner.withPropertyValues("zhe.log.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(LogPrintAspect.class);
            assertThat(context).doesNotHaveBean(RequestLogAspect.class);
            assertThat(context).doesNotHaveBean(ResponseLogAspect.class);
        });
    }

    @Test
    void orderPropertiesBind() {
        runner.withPropertyValues("zhe.log.log-print-order=2", "zhe.log.response-log-order=4").run(context -> {
            assertThat(context.getBean(LogPrintAspect.class).getOrder()).isEqualTo(2);
            assertThat(context.getBean(ResponseLogAspect.class).getOrder()).isEqualTo(4);
        });
    }
}
