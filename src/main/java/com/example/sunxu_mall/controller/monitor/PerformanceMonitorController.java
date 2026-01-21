package com.example.sunxu_mall.controller.monitor;

import com.example.sunxu_mall.annotation.NoLogin;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 性能监控端点
 * 用于查看缓存命中率、接口耗时等性能指标
 *
 * @author sunxu
 * @date 2026/1/24
 */
@Slf4j
@Tag(name = "性能监控", description = "性能监控接口")
@RestController
@RequestMapping("/monitor/performance")
public class PerformanceMonitorController {

    private final CacheManager cacheManager;

    public PerformanceMonitorController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 获取缓存统计信息
     */
    @NoLogin
    @Operation(summary = "获取缓存统计信息", description = "返回各缓存的命中率、驱逐次数等指标")
    @GetMapping("/cache-stats")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> result = new HashMap<>();
        
        for (String cacheName : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                CaffeineCache caffeineCache = (CaffeineCache) cache;
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                CacheStats stats = nativeCache.stats();
                
                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("hitCount", stats.hitCount());
                cacheInfo.put("missCount", stats.missCount());
                cacheInfo.put("hitRate", String.format("%.2f%%", stats.hitRate() * 100));
                cacheInfo.put("evictionCount", stats.evictionCount());
                cacheInfo.put("loadSuccessCount", stats.loadSuccessCount());
                cacheInfo.put("loadFailureCount", stats.loadFailureCount());
                cacheInfo.put("averageLoadPenalty", String.format("%.2fms", stats.averageLoadPenalty() / 1_000_000.0));
                cacheInfo.put("estimatedSize", nativeCache.estimatedSize());
                
                result.put(cacheName, cacheInfo);
            }
        }
        
        return result;
    }
}
