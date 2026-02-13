package com.example.sunxu_mall.interceptor;

import com.example.sunxu_mall.annotation.Idempotency;
import com.example.sunxu_mall.errorcode.ErrorCode;
import com.example.sunxu_mall.idempotency.IdempotencyKeyResolver;
import com.example.sunxu_mall.idempotency.IdempotencyProperties;
import com.example.sunxu_mall.util.ApiResult;
import com.example.sunxu_mall.util.ApiResultUtil;
import com.example.sunxu_mall.util.JsonUtil;
import com.example.sunxu_mall.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 幂等/防重复提交拦截器
 * <p>
 * 对所有写请求（POST/PUT/DELETE）进行幂等校验：
 * <ol>
 *   <li>通过 {@link IdempotencyKeyResolver} 解析幂等 Key</li>
 *   <li>使用 Redis SETNX + TTL 原子抢占</li>
 *   <li>抢占失败 → 判定为重复提交，返回 409</li>
 * </ol>
 *
 * @author sunxu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final IdempotencyProperties properties;
    private final IdempotencyKeyResolver keyResolver;
    private final RedisUtil redisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 全局开关
        if (!properties.isEnabled()) {
            return true;
        }

        Idempotency idempotency = resolveIdempotencyAnnotation(handler);
        if (Objects.isNull(idempotency)) {
            return true;
        }

        // 解析幂等 Key
        String idempotencyKey = keyResolver.resolve(request);
        int ttlSeconds = resolveTtlSeconds(idempotency);

        // 原子抢占：SETNX + TTL
        boolean acquired = redisUtil.setIfAbsent(
                idempotencyKey,
                "1",
                ttlSeconds,
                TimeUnit.SECONDS
        );

        if (!acquired) {
            log.info("Duplicate submit detected, key={}, uri={}, method={}",
                    idempotencyKey, request.getRequestURI(), request.getMethod());
            writeErrorResponse(response);
            return false;
        }

        return true;
    }

    /**
     * 解析接口上的防重注解（方法优先，其次类）
     */
    private Idempotency resolveIdempotencyAnnotation(Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return null;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Idempotency methodAnnotation = handlerMethod.getMethodAnnotation(Idempotency.class);
        if (Objects.nonNull(methodAnnotation)) {
            return methodAnnotation;
        }
        return handlerMethod.getBeanType().getAnnotation(Idempotency.class);
    }

    /**
     * 解析防重 TTL（秒）
     */
    private int resolveTtlSeconds(Idempotency idempotency) {
        int annotationTtlSeconds = idempotency.ttlSeconds();
        if (annotationTtlSeconds > 0) {
            return annotationTtlSeconds;
        }
        return properties.getDefaultTtlSeconds();
    }

    /**
     * 向客户端写入重复提交的错误响应
     */
    private void writeErrorResponse(HttpServletResponse response) throws IOException {
        ErrorCode errorCode = ErrorCode.DUPLICATE_SUBMIT;
        ApiResult<Void> result = ApiResultUtil.error(errorCode.getCode(), errorCode.getMessage());

        response.setStatus(errorCode.getHttpStatus());
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(
                JsonUtil.toJsonStr(result).getBytes(StandardCharsets.UTF_8)
        );
    }
}
