package com.example.sunxu_mall.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Objects;

/**
 * 接口耗时统计拦截器
 * 记录每个接口的请求耗时，超过阈值时输出警告日志
 *
 * @author sunxu
 * @date 2026/1/24
 */
@Slf4j
@Component
public class PerformanceInterceptor implements HandlerInterceptor {

    private static final String START_TIME_KEY = "perf_start_time";

    /**
     * 慢接口阈值（毫秒），超过此值输出警告日志
     */
    @Value("${mall.performance.slow-api-threshold-ms:1000}")
    private long slowApiThresholdMs;

    /**
     * 是否启用性能日志
     */
    @Value("${mall.performance.enabled:true}")
    private boolean enabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (enabled) {
            request.setAttribute(START_TIME_KEY, System.currentTimeMillis());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                 Object handler, Exception ex) {
        if (!enabled) {
            return;
        }

        Long startTime = (Long) request.getAttribute(START_TIME_KEY);
        if (Objects.isNull(startTime)) {
            return;
        }

        long duration = System.currentTimeMillis() - startTime;
        String uri = request.getRequestURI();
        String method = request.getMethod();
        int status = response.getStatus();
        String traceId = MDC.get("traceId");

        if (duration >= slowApiThresholdMs) {
            log.warn("[SlowAPI] traceId={}, method={}, uri={}, status={}, duration={}ms (threshold={}ms)",
                    traceId, method, uri, status, duration, slowApiThresholdMs);
        } else if (log.isDebugEnabled()) {
            log.debug("[API] traceId={}, method={}, uri={}, status={}, duration={}ms",
                    traceId, method, uri, status, duration);
        }
    }
}
