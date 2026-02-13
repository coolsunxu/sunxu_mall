package com.example.sunxu_mall.idempotency;

import com.example.sunxu_mall.filter.CachedBodyRequestWrapper;
import com.example.sunxu_mall.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 幂等 Key 解析器
 * <p>
 * 解析优先级：
 * 1. 请求 Header 中的 Idempotency-Key（客户端显式传入）
 * 2. 自动生成：userId + httpMethod + requestURI + bodyHash（短窗口防双击）
 *
 * @author sunxu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyKeyResolver {

    private final IdempotencyProperties properties;

    /**
     * 从请求中解析幂等 Key
     *
     * @param request HTTP 请求
     * @return 带前缀的 Redis Key
     */
    public String resolve(HttpServletRequest request) {
        String prefix = properties.getKeyPrefix();

        // 优先从 Header 获取
        String headerKey = request.getHeader(properties.getHeaderName());
        if (StringUtils.isNotBlank(headerKey)) {
            return prefix + "h:" + headerKey;
        }

        // Fallback：userId + method + uri + bodyHash
        String userId = getCurrentUserIdSafe();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String bodyHash = computeBodyHash(request);

        String rawKey = userId + ":" + method + ":" + uri + ":" + bodyHash;
        return prefix + "a:" + md5Hex(rawKey);
    }

    /**
     * 安全获取当前用户 ID（未登录时返回 "anon"）
     */
    private String getCurrentUserIdSafe() {
        try {
            if (SecurityUtil.isAuthenticated()) {
                return String.valueOf(SecurityUtil.getCurrentUserId());
            }
        } catch (Exception e) {
            log.debug("Failed to get current user id for idempotency key, using anonymous");
        }
        return "anon";
    }

    /**
     * 计算请求体的 MD5 摘要
     */
    private String computeBodyHash(HttpServletRequest request) {
        if (request instanceof CachedBodyRequestWrapper) {
            byte[] body = ((CachedBodyRequestWrapper) request).getCachedBody();
            if (body != null && body.length > 0) {
                return md5Hex(new String(body, StandardCharsets.UTF_8));
            }
        }
        return "empty";
    }

    /**
     * 计算字符串的 MD5 十六进制摘要
     *
     * @param input 原始字符串
     * @return MD5 十六进制字符串（32位小写）
     */
    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 在所有标准 JVM 中都支持，此异常不应发生
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }
}
