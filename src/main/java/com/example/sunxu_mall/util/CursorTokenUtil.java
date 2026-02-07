package com.example.sunxu_mall.util;

import com.example.sunxu_mall.dto.CursorState;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * @author sunxu
 */
@Slf4j
public class CursorTokenUtil {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private CursorTokenUtil() {
        // Utility class
    }

    /**
     * Encode cursor state to Base64 token
     * @param state cursor state
     * @return Base64 encoded token or null if state is null
     */
    public static String encode(CursorState state) {
        if (Objects.isNull(state)) {
            return null;
        }
        try {
            String json = JsonUtil.toJsonStr(state);
            return ENCODER.encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Failed to encode cursor state", e);
            return null;
        }
    }

    /**
     * Decode Base64 token to cursor state
     * @param token Base64 encoded token
     * @return cursor state or null if token is null/invalid
     */
    public static CursorState decode(String token) {
        if (Objects.isNull(token) || token.trim().isEmpty()) {
            return null;
        }
        try {
            byte[] decodedBytes = DECODER.decode(token);
            String json = new String(decodedBytes, StandardCharsets.UTF_8);
            return JsonUtil.parseObject(json, CursorState.class);
        } catch (Exception e) {
            log.warn("Failed to decode cursor token: {}", token, e);
            return null;
        }
    }

    /**
     * Create initial cursor state
     * @param pageNum initial page number
     * @param pageSize page size
     * @return cursor state
     */
    public static CursorState createInitialState(Integer pageNum, Integer pageSize) {
        if (Objects.isNull(pageNum)) {
            pageNum = 1;
        }
        if (Objects.isNull(pageSize)) {
            pageSize = 10;
        }
        return new CursorState(pageNum, pageSize);
    }
}