package com.example.sunxu_mall.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 幂等过滤器：对写请求（POST/PUT/DELETE）包装 Request，使 Body 可重复读取
 * <p>
 * 该 Filter 在 {@link TraceIdFilter} 之后执行，将 HttpServletRequest 包装为
 * {@link CachedBodyRequestWrapper}，以便后续拦截器可读取 Body 计算幂等 Key，
 * 同时下游 Controller 仍可正常反序列化请求体。
 * <p>
 * 跳过 multipart/form-data 请求（文件上传），避免将大文件缓存到内存。
 *
 * @author sunxu
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class IdempotencyFilter implements Filter {

    private static final String MULTIPART_PREFIX = "multipart/";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            if (isWriteMethod(httpRequest) && !isMultipart(httpRequest)) {
                CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(httpRequest);
                chain.doFilter(wrapper, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 判断是否为写方法
     */
    private boolean isWriteMethod(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    /**
     * 判断是否为 multipart 请求（文件上传）
     */
    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith(MULTIPART_PREFIX);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 不需要初始化
    }

    @Override
    public void destroy() {
        // 不需要销毁
    }
}
