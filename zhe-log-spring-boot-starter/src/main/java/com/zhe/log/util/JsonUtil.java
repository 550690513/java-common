package com.zhe.log.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 日志专用 JSON 工具：所有方法都不会抛异常，序列化失败时降级为占位文本，保证日志打印不影响业务执行。
 */
public final class JsonUtil {

    public static final String PATTERN_DATE_TIME = "yyyy-MM-dd HH:mm:ss";

    private static final ObjectMapper MAPPER;

    static {
        DateTimeFormatter format = DateTimeFormatter.ofPattern(PATTERN_DATE_TIME);
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(Instant.class, new InstantSerializer(format));
        MAPPER = JsonMapper.builder(new JsonFactory())
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build()
                .registerModule(javaTimeModule);
    }

    private JsonUtil() {
    }

    /**
     * 参数序列化：优先使用形参名（需要编译时开启 -parameters），否则退化为参数数组。
     * servlet 请求/响应、multipart 文件、流等类型只打印占位符，不做序列化。
     */
    public static String argsToString(String[] parameterNames, Object[] args) {
        try {
            boolean named = parameterNames != null
                    && args != null
                    && parameterNames.length == args.length
                    && parameterNames.length > 0;
            JsonNode node = named
                    ? namedArgsNode(parameterNames, args)
                    : positionalArgsNode(args == null ? new Object[0] : args);
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            return "[args unserializable]";
        }
    }

    /**
     * 单值序列化：失败时降级为 "[unserializable: 类名]" 占位文本。
     */
    public static String toLogString(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "[unserializable: " + obj.getClass().getName() + "]";
        }
    }

    private static JsonNode namedArgsNode(String[] parameterNames, Object[] args) {
        ObjectNode node = MAPPER.createObjectNode();
        for (int i = 0; i < parameterNames.length; i++) {
            String name = parameterNames[i];
            Object value = args[i];
            node.set(name, valueNode(name, value));
        }
        return node;
    }

    private static JsonNode positionalArgsNode(Object[] args) {
        ArrayNode node = MAPPER.createArrayNode();
        for (int i = 0; i < args.length; i++) {
            node.add(valueNode("arg" + i, args[i]));
        }
        return node;
    }

    private static JsonNode valueNode(String name, Object value) {
        if (isSkippable(value)) {
            String simpleName = value.getClass().getSimpleName();
            return new TextNode("[" + (simpleName.isEmpty() ? value.getClass().getName() : simpleName) + " skipped]");
        }
        try {
            return MAPPER.valueToTree(value);
        } catch (Exception e) {
            return new TextNode("[unserializable: "
                    + (value == null ? "null" : value.getClass().getName()) + "]");
        }
    }

    /**
     * 通过类名前缀过滤 servlet（javax/jakarta）与 multipart 类型，兼容 Spring Boot 2.x 与 3.x，无需编译期依赖。
     */
    private static boolean isSkippable(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof InputStream) {
            return true;
        }
        String className = value.getClass().getName();
        return className.startsWith("javax.servlet.")
                || className.startsWith("jakarta.servlet.")
                || className.startsWith("org.springframework.web.multipart.");
    }

    static class InstantSerializer extends StdSerializer<Instant> {

        private final DateTimeFormatter format;

        InstantSerializer(DateTimeFormatter format) {
            super(Instant.class);
            this.format = format;
        }

        @Override
        public void serialize(Instant value, com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            gen.writeString(format.format(value.atZone(ZoneId.systemDefault())));
        }
    }
}
