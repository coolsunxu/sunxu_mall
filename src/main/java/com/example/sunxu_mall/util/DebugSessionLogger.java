package com.example.sunxu_mall.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 调试会话日志工具：按 NDJSON 格式写入本地文件。
 *
 * @author sunxu
 */
public final class DebugSessionLogger {

    private static final String SESSION_ID = "faa268";
    private static final String LOG_PATH = "debug-faa268.log";

    private DebugSessionLogger() {
        // Utility class
    }

    public static void log(String runId, String hypothesisId, String location, String message, Map<String, Object> data) {
        Map<String, Object> payload = new LinkedHashMap<>(8);
        payload.put("sessionId", SESSION_ID);
        payload.put("runId", runId);
        payload.put("hypothesisId", hypothesisId);
        payload.put("location", location);
        payload.put("message", message);
        payload.put("data", data);
        payload.put("timestamp", System.currentTimeMillis());
        String line = JsonUtil.toJsonStr(payload);
        if (Objects.isNull(line)) {
            return;
        }
        try {
            Files.writeString(Path.of(LOG_PATH), line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
        } catch (Exception ignored) {
            // Debug logging must not affect business flow.
        }
    }
}
