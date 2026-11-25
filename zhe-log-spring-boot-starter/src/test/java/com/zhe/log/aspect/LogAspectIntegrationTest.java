package com.zhe.log.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zhe.log.annotation.LogPrint;
import com.zhe.log.annotation.RequestLog;
import com.zhe.log.annotation.ResponseLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = LogAspectIntegrationTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LogAspectIntegrationTest {

    @Autowired
    private OuterService outerService;
    @Autowired
    private BrokenService brokenService;
    @Autowired
    private RequestService requestService;
    @Autowired
    private ResponseService responseService;

    private final ListAppender<ILoggingEvent> printAppender = new ListAppender<>();
    private final ListAppender<ILoggingEvent> requestAppender = new ListAppender<>();
    private final ListAppender<ILoggingEvent> responseAppender = new ListAppender<>();

    @BeforeEach
    void attachAppenders() {
        attach(printAppender, LogPrintAspect.class);
        attach(requestAppender, RequestLogAspect.class);
        attach(responseAppender, ResponseLogAspect.class);
    }

    @AfterEach
    void detachAppenders() {
        ((Logger) LoggerFactory.getLogger(LogPrintAspect.class)).detachAndStopAllAppenders();
        ((Logger) LoggerFactory.getLogger(RequestLogAspect.class)).detachAndStopAllAppenders();
        ((Logger) LoggerFactory.getLogger(ResponseLogAspect.class)).detachAndStopAllAppenders();
    }

    private void attach(ListAppender<ILoggingEvent> appender, Class<?> loggerClass) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerClass);
        appender.setContext((ch.qos.logback.classic.LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();
        logger.addAppender(appender);
    }

    private List<String> messages(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
    }

    @Test
    void logPrintLogsParamsOfEachLevelAndResultOnlyAtOutermost() {
        String result = outerService.handle("tom");

        assertThat(result).isEqualTo("TOM!");
        List<String> logs = messages(printAppender);
        assertThat(logs).anyMatch(m -> m.contains("OuterService#handle() param=") && m.contains("\"name\":\"tom\""));
        assertThat(logs).anyMatch(m -> m.contains("InnerService#process() param=") && m.contains("\"input\":\"tom\""));
        assertThat(logs).filteredOn(m -> m.contains("result=")).hasSize(1);
        assertThat(logs).anyMatch(m -> m.contains("result=\"TOM!\"") && m.matches(".*cost: \\d+ms"));
    }

    @Test
    void logPrintLogsThrowAndCleansDepthState() {
        assertThatThrownBy(() -> brokenService.explode("boom"))
                .isInstanceOf(IllegalStateException.class);

        List<String> logs = messages(printAppender);
        assertThat(logs).filteredOn(m -> m.contains("throw=")).hasSize(1);
        assertThat(logs).anyMatch(m -> m.contains("throw=java.lang.IllegalStateException") && m.matches(".*cost: \\d+ms"));

        // 异常后 ThreadLocal 已清理，下一次调用仍能正常打印 result
        assertThat(brokenService.safe()).isEqualTo("ok");
        assertThat(messages(printAppender)).anyMatch(m -> m.contains("BrokenService#safe() result="));
    }

    @Test
    void requestLogLogsParamsAndThrow() {
        assertThat(requestService.greet("alice")).isEqualTo("hi alice");
        assertThatThrownBy(() -> requestService.fail()).isInstanceOf(IllegalStateException.class);

        List<String> logs = messages(requestAppender);
        assertThat(logs).anyMatch(m -> m.contains("RequestService#greet() param=") && m.contains("\"name\":\"alice\""));
        assertThat(logs).anyMatch(m -> m.contains("RequestService#fail() throw=java.lang.IllegalStateException"));
    }

    @Test
    void responseLogSupportsClassLevelAnnotation() {
        assertThat(responseService.echo("data")).isEqualTo("data");

        List<String> logs = messages(responseAppender);
        assertThat(logs).anyMatch(m -> m.contains("ResponseService#echo() result=") && m.contains("\"data\""));
    }

    @EnableAutoConfiguration
    static class TestApp {

        @org.springframework.context.annotation.Bean
        public OuterService outerService(InnerService innerService) {
            // 从容器注入，保证拿到的是被切面代理后的 InnerService
            return new OuterService(innerService);
        }

        @org.springframework.context.annotation.Bean
        public InnerService innerService() {
            return new InnerService();
        }

        @org.springframework.context.annotation.Bean
        public BrokenService brokenService() {
            return new BrokenService();
        }

        @org.springframework.context.annotation.Bean
        public RequestService requestService() {
            return new RequestService();
        }

        @org.springframework.context.annotation.Bean
        public ResponseService responseService() {
            return new ResponseService();
        }
    }

    static class OuterService {
        private final InnerService innerService;

        OuterService(InnerService innerService) {
            this.innerService = innerService;
        }

        @LogPrint
        public String handle(String name) {
            return innerService.process(name) + "!";
        }
    }

    static class InnerService {

        @LogPrint
        public String process(String input) {
            return input.toUpperCase();
        }
    }

    static class BrokenService {

        @LogPrint
        public String explode(String message) {
            throw new IllegalStateException(message);
        }

        @LogPrint
        public String safe() {
            return "ok";
        }
    }

    static class RequestService {

        @RequestLog
        public String greet(String name) {
            return "hi " + name;
        }

        @RequestLog
        public String fail() {
            throw new IllegalStateException("fail");
        }
    }

    @ResponseLog
    static class ResponseService {

        public String echo(String data) {
            return data;
        }
    }
}
