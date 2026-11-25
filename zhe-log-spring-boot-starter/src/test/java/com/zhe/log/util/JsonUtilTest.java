package com.zhe.log.util;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUtilTest {

    @Test
    void argsToStringWithParameterNames() {
        String json = JsonUtil.argsToString(new String[]{"name", "age"}, new Object[]{"tom", 18});
        assertEquals("{\"name\":\"tom\",\"age\":18}", json);
    }

    @Test
    void argsToStringFallsBackToPositionalWhenNamesMissing() {
        String json = JsonUtil.argsToString(new String[0], new Object[]{"a", 1});
        assertEquals("[\"a\",1]", json);
    }

    @Test
    void argsToStringWithNullValues() {
        String json = JsonUtil.argsToString(new String[]{"a", "b"}, new Object[]{null, null});
        assertEquals("{\"a\":null,\"b\":null}", json);
    }

    @Test
    void argsToStringSkipsStreamType() {
        String json = JsonUtil.argsToString(new String[]{"in", "ok"}, new Object[]{new EmptyStream(), "v"});
        assertTrue(json.contains("[EmptyStream skipped]"));
        assertTrue(json.contains("\"ok\":\"v\""));
    }

    @Test
    void argsToStringSkipsAnonymousClassWithFallbackName() {
        InputStream anonymous = new InputStream() {
            @Override
            public int read() {
                return -1;
            }
        };
        String json = JsonUtil.argsToString(new String[]{"in"}, new Object[]{anonymous});
        // 匿名类没有 SimpleName，降级打印全限定类名
        assertTrue(json.contains("[com.zhe.log.util.JsonUtilTest$"));
        assertTrue(json.contains(" skipped]"));
    }

    static class EmptyStream extends InputStream {
        @Override
        public int read() {
            return -1;
        }
    }

    @Test
    void argsToStringSkipsServletTypeByClassNamePrefix() {
        // FakeRequest 定义在 jakarta.servlet.http 包下（见同目录），类名前缀命中即跳过
        String json = JsonUtil.argsToString(new String[]{"request", "ok"},
                new Object[]{new jakarta.servlet.http.FakeRequest(), "v"});
        assertTrue(json.contains("[FakeRequest skipped]"));
        assertTrue(json.contains("\"ok\":\"v\""));
    }

    @Test
    void argsToStringFallsBackPerValueOnFailure() {
        String json = JsonUtil.argsToString(new String[]{"bad", "good"},
                new Object[]{new BrokenGetter(), "ok"});
        assertTrue(json.contains("[unserializable:"));
        assertTrue(json.contains("\"good\":\"ok\""));
    }

    @Test
    void argsToStringFormatsInstant() {
        Instant instant = Instant.parse("2026-01-02T03:04:05Z");
        Map<String, Object> arg = new HashMap<>();
        arg.put("time", instant);
        String json = JsonUtil.argsToString(new String[]{"arg"}, new Object[]{arg});
        assertFalse(json.contains("T03:04:05Z"));
        assertTrue(json.contains("\"time\":\"2026-01-02"));
    }

    @Test
    void toLogStringHandlesNullAndFailure() {
        assertEquals("null", JsonUtil.toLogString(null));
        assertEquals("\"ok\"", JsonUtil.toLogString("ok"));
        assertTrue(JsonUtil.toLogString(new BrokenGetter()).contains("[unserializable:"));
    }

    @Test
    void toLogStringFormatsInstant() {
        String json = JsonUtil.toLogString(Arrays.asList(Instant.parse("2026-01-02T03:04:05Z")));
        assertFalse(json.contains("T03:04:05Z"));
        assertTrue(json.contains("2026-01-02"));
    }

    static class BrokenGetter {
        public String getName() {
            throw new IllegalStateException("boom");
        }
    }
}
