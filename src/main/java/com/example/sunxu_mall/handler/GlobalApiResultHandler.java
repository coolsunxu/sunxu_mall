package com.example.sunxu_mall.handler;

import com.example.sunxu_mall.util.ApiResult;
import com.example.sunxu_mall.util.ApiResultUtil;
import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * @author sunxu
 * @date 2025/12/24 19.52
 */
@ControllerAdvice
public class GlobalApiResultHandler implements ResponseBodyAdvice<Object> {
    public static final String URL_PREFIX = "/v1";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = sra.getRequest();
        String requestURI = request.getRequestURI();
        return matchUrl(requestURI);
    }

    private boolean matchUrl(String uri) {
        if (StringUtils.isBlank(uri)) {
            return false;
        }
        return uri.contains(URL_PREFIX);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResult) {
            return body;
        }
        return ApiResultUtil.success(body);
    }
}
