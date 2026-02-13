package com.example.sunxu_mall.filter;

import lombok.Getter;
import org.springframework.util.StreamUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 可重复读取 Body 的 HttpServletRequest 包装器
 * <p>
 * 在构造时一次性读取完整请求体并缓存，后续通过 {@link #getInputStream()} 或 {@link #getReader()}
 * 可多次读取缓存内容，避免 InputStream 只能消费一次的问题。
 *
 * @author sunxu
 */
@Getter
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    /**
     * -- GETTER --
     *  获取缓存的请求体字节数组
     *
     */
    private final byte[] cachedBody;

    public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // 不需要异步支持
            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

}
