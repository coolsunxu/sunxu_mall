package com.example.sunxu_mall.config.beans;

import com.example.sunxu_mall.config.props.OkHttpConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp Auto Configuration
 * Provides pre-configured OkHttpClient Bean with connection pool, timeout, SSL configuration, etc.
 *
 * @author sunxu
 * @version 1.0
 * @date 2025/12/31 18:36
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(OkHttpConfig.class)
public class OkHttpAutoConfiguration {

    /**
     * Create a well-configured OkHttpClient Bean
     * Includes connection pool management, timeout settings, SSL configuration and request logging
     *
     * @param props OkHttp configuration properties
     * @return Configured OkHttpClient instance
     */
    @Bean
    public OkHttpClient okHttpClient(OkHttpConfig props) {
        try {
            // Create connection pool configuration
            ConnectionPool pool = new ConnectionPool(
                    props.getConnectionPool().getMaxIdleConnections(),
                    props.getConnectionPool().getKeepAliveDuration().getSeconds(),
                    TimeUnit.SECONDS
            );

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(props.getConnectTimeout())
                    .readTimeout(props.getReadTimeout())
                    .writeTimeout(props.getWriteTimeout())
                    .followRedirects(props.isFollowRedirects())
                    .followSslRedirects(props.isFollowSslRedirects())
                    .connectionPool(pool)
                    .addInterceptor(new LoggingInterceptor()) // Add logging interceptor
                    ;

            // 默认使用 JVM/系统默认的 TLS 校验；仅当显式配置开启时才启用“不安全信任所有”
            if (props.isInsecureTrustAll()) {
                log.warn("OkHttp insecureTrustAll ENABLED. This should NOT be used in production!");
                final TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[]{};
                            }
                        }
                };

                final SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
                builder.hostnameVerifier((hostname, session) -> true);
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to initialize OkHttpClient", e);
            throw new RuntimeException("Failed to initialize OkHttpClient", e);
        }
    }

    /**
     * Request logging interceptor
     * Records basic request and response information for debugging and monitoring
     */
    public static class LoggingInterceptor implements Interceptor {
        @NotNull
        @Override
        public Response intercept(Chain chain) throws IOException {
            okhttp3.Request request = chain.request();
            long startTime = System.currentTimeMillis();

            log.debug("OkHttp Request: {} {}", request.method(), request.url());

            Response response = chain.proceed(request);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("OkHttp Response: {} in {}ms", response.code(), duration);

            return response;
        }
    }
}


