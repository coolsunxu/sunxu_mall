package com.example.sunxu_mall.config.beans;

import com.example.sunxu_mall.config.props.ThreadPoolConfig;
import com.example.sunxu_mall.errorcode.ErrorCode;
import com.example.sunxu_mall.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步配置类
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    private final ThreadPoolConfig threadPoolProperties;

    @Bean(name = "loginEventExecutor")
    public Executor loginEventExecutor() {
        ThreadPoolConfig.PoolConfig config = null;
        if (threadPoolProperties.getPools() != null) {
            config = threadPoolProperties.getPools().get("login");
        }

        if (Objects.isNull(config)) {
            throw new BusinessException(ErrorCode.CONFIG_MISSING.getCode(), "Thread pool configuration 'login' is required");
        }
        
        log.info("Initializing loginEventExecutor with config: {}", config);

        return getThreadPoolTaskExecutor(config);
    }

    @Bean(name = "commonTaskExecutor")
    public Executor commonTaskExecutor() {
        ThreadPoolConfig.PoolConfig config = null;
        if (threadPoolProperties.getPools() != null) {
            config = threadPoolProperties.getPools().get("common");
        }

        if (Objects.isNull(config)) {
            log.warn("Thread pool configuration 'common' is missing, using default.");
            config = new ThreadPoolConfig.PoolConfig();
            config.setCoreSize(2);
            config.setMaxSize(5);
            config.setQueueCapacity(200);
            config.setKeepAliveSeconds(60);
            config.setThreadNamePrefix("async-common-");
        }

        log.info("Initializing commonTaskExecutor with config: {}", config);

        return getThreadPoolTaskExecutor(config);
    }

    @Override
    public Executor getAsyncExecutor() {
        return commonTaskExecutor();
    }

    @NotNull
    private static ThreadPoolTaskExecutor getThreadPoolTaskExecutor(ThreadPoolConfig.PoolConfig config) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getCoreSize());
        executor.setMaxPoolSize(config.getMaxSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setKeepAliveSeconds(config.getKeepAliveSeconds());
        executor.setThreadNamePrefix(config.getThreadNamePrefix());
        // 拒绝策略：由调用线程处理，防止丢弃
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error("Async method execution exception: method={}, params={}", method.getName(), params, ex);
    }
}
