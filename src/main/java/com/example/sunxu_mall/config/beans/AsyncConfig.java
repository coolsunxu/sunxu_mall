package com.example.sunxu_mall.config.beans;

import com.example.sunxu_mall.config.props.ThreadPoolConfig;
import com.example.sunxu_mall.context.AuditContextHolder;
import com.example.sunxu_mall.context.AuditUser;
import com.example.sunxu_mall.errorcode.ErrorCode;
import com.example.sunxu_mall.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步配置类
 *
 * @author sunxu
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
        if (Objects.nonNull(threadPoolProperties.getPools())) {
            config = threadPoolProperties.getPools().get("login");
        }

        // 如果缺少配置，服务应该无法启动，不需要配置兜底策略
        if (Objects.isNull(config)) {
            throw new BusinessException(ErrorCode.CONFIG_MISSING.getCode(), "Thread pool configuration 'login' is required");
        }
        
        log.info("Initializing loginEventExecutor with config: {}", config);

        return getThreadPoolTaskExecutor(config);
    }

    @Bean(name = "commonTaskExecutor")
    public Executor commonTaskExecutor() {
        ThreadPoolConfig.PoolConfig config = null;
        if (Objects.nonNull(threadPoolProperties.getPools())) {
            config = threadPoolProperties.getPools().get("common");
        }

        // 如果缺少配置，服务应该无法启动，不需要配置兜底策略
        if (Objects.isNull(config)) {
            throw new BusinessException(ErrorCode.CONFIG_MISSING.getCode(), "Thread pool configuration 'common' is required");
        }

        log.info("Initializing commonTaskExecutor with config: {}", config);

        return getThreadPoolTaskExecutor(config);
    }

    /**
     * 导出任务专用线程池
     * 独立于 commonTaskExecutor，避免导出任务占满通用线程池影响其他异步事件
     */
    @Bean(name = "exportExecutor")
    public Executor exportExecutor() {
        ThreadPoolConfig.PoolConfig config = null;
        if (Objects.nonNull(threadPoolProperties.getPools())) {
            config = threadPoolProperties.getPools().get("export");
        }

        // 如果缺少配置，服务应该无法启动，不需要配置兜底策略
        if (Objects.isNull(config)) {
            throw new BusinessException(ErrorCode.CONFIG_MISSING.getCode(), "Thread pool configuration 'export' is required");
        }

        log.info("Initializing exportExecutor with config: {}", config);

        return getThreadPoolTaskExecutor(config);
    }

    /**
     * 通知任务专用线程池
     * 独立于其他线程池，避免通知任务影响其他业务
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolConfig.PoolConfig config = null;
        if (Objects.nonNull(threadPoolProperties.getPools())) {
            config = threadPoolProperties.getPools().get("notification");
        }

        // 如果缺少配置，服务应该无法启动，不需要配置兜底策略
        if (Objects.isNull(config)) {
            throw new BusinessException(ErrorCode.CONFIG_MISSING.getCode(), "Thread pool configuration 'notification' is required");
        }
        
        log.info("Initializing notificationExecutor with config: {}", config);

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
        executor.setTaskDecorator(contextCopyingTaskDecorator());
        // 拒绝策略：由调用线程处理，防止丢弃
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    private static TaskDecorator contextCopyingTaskDecorator() {
        return runnable -> {
            SecurityContext securityContext = SecurityContextHolder.getContext();
            AuditUser auditUser = AuditContextHolder.get();
            return () -> {
                SecurityContext previousSecurityContext = SecurityContextHolder.getContext();
                AuditUser previousAuditUser = AuditContextHolder.get();
                try {
                    SecurityContextHolder.setContext(securityContext);
                    if (Objects.nonNull(auditUser)) {
                        AuditContextHolder.set(auditUser);
                    } else {
                        AuditContextHolder.clear();
                    }
                    runnable.run();
                } finally {
                    SecurityContextHolder.setContext(previousSecurityContext);
                    if (Objects.nonNull(previousAuditUser)) {
                        AuditContextHolder.set(previousAuditUser);
                    } else {
                        AuditContextHolder.clear();
                    }
                }
            };
        };
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error("Async method execution exception: method={}, params={}", method.getName(), params, ex);
    }
}
