package com.example.sunxu_mall.service.impl;

import com.example.sunxu_mall.config.beans.CacheConfig;
import com.example.sunxu_mall.config.props.IpCityConfig;
import com.example.sunxu_mall.dto.ip.IpCityDTO;
import com.example.sunxu_mall.service.IpCityService;
import com.example.sunxu_mall.util.JsonUtil;
import com.example.sunxu_mall.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 复合IP查询服务 (多级缓存 + 离线库 + 降级)
 * 
 * 缓存策略：
 * - L1: Caffeine 本地缓存（高速读取）
 * - L2: Redis 分布式缓存（跨实例共享）
 * 
 * 防护策略：
 * - 穿透保护：对查询不到结果的 IP 做短 TTL 负缓存
 * - 击穿保护：使用本地互斥锁防止同一时刻大量请求回源
 */
@Slf4j
@Service
@Primary
public class CompositeIpCityService implements IpCityService {

    private final IpCityService localService;
    private final IpCityService remoteService;
    private final CacheManager cacheManager;
    private final RedisUtil redisUtil;
    private final IpCityConfig ipCityConfig;
    
    private static final String REDIS_PREFIX = "ip:city:";
    
    /**
     * 负缓存标记（用于穿透保护）
     */
    private static final String NULL_CACHE_VALUE = "__NULL__";
    
    /**
     * 负缓存 TTL（秒），防止无效 IP 频繁查询回源
     */
    private static final long NULL_CACHE_TTL_SECONDS = 300;
    
    /**
     * 本地锁映射（用于击穿保护）
     * 注意：这是本地锁，仅防止单实例内的并发回源
     * 对于分布式场景，可以使用 Redis 分布式锁
     */
    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    public CompositeIpCityService(
            @Qualifier("localGeoLite2IpCityService") IpCityService localService,
            @Qualifier("geoLite2IpCityApiService") IpCityService remoteService,
            CacheManager cacheManager,
            RedisUtil redisUtil,
            IpCityConfig ipCityConfig) {
        this.localService = localService;
        this.remoteService = remoteService;
        this.cacheManager = cacheManager;
        this.redisUtil = redisUtil;
        this.ipCityConfig = ipCityConfig;
    }

    @Override
    public IpCityDTO getCityByIp(String ip) {
        if (!StringUtils.hasText(ip)) return null;

        // 1. Check L1 Cache (Caffeine)
        Cache cache = cacheManager.getCache(CacheConfig.IP_CITY_CACHE);
        if (Objects.nonNull(cache)) {
            Cache.ValueWrapper wrapper = cache.get(ip);
            if (Objects.nonNull(wrapper)) {
                Object value = wrapper.get();
                if (NULL_CACHE_VALUE.equals(value)) {
                    // 穿透保护：负缓存命中
                    log.debug("[IP-Cache] Hit L1 null-cache for IP={}", ip);
                    return null;
                }
                if (value instanceof IpCityDTO) {
                    log.debug("[IP-Cache] Hit L1 for IP={}", ip);
                    return (IpCityDTO) value;
                }
            }
        }

        // 2. Check L2 Cache (Redis)
        String redisKey = REDIS_PREFIX + ip;
        try {
            String redisJson = redisUtil.get(redisKey);
            if (StringUtils.hasText(redisJson)) {
                if (NULL_CACHE_VALUE.equals(redisJson)) {
                    // 穿透保护：负缓存命中
                    log.debug("[IP-Cache] Hit L2 null-cache for IP={}", ip);
                    // Write back to L1
                    if (Objects.nonNull(cache)) cache.put(ip, NULL_CACHE_VALUE);
                    return null;
                }
                IpCityDTO cached = JsonUtil.fromJson(redisJson, IpCityDTO.class);
                if (Objects.nonNull(cached)) {
                    log.debug("[IP-Cache] Hit L2 for IP={}", ip);
                    // Write back to L1
                    if (Objects.nonNull(cache)) cache.put(ip, cached);
                    return cached;
                }
            }
        } catch (Exception e) {
            log.warn("[IP-Cache] Redis error for IP={}", ip, e);
        }

        // 3. 击穿保护：使用本地锁防止并发回源
        return queryWithLock(ip, cache, redisKey);
    }
    
    /**
     * 带锁的查询方法，防止缓存击穿
     */
    private IpCityDTO queryWithLock(String ip, Cache cache, String redisKey) {
        // 获取或创建锁（使用 computeIfAbsent 保证原子性）
        ReentrantLock lock = lockMap.computeIfAbsent(ip, k -> new ReentrantLock());
        
        lock.lock();
        try {
            // Double check：加锁后再次检查缓存
            if (Objects.nonNull(cache)) {
                Cache.ValueWrapper wrapper = cache.get(ip);
                if (Objects.nonNull(wrapper)) {
                    Object value = wrapper.get();
                    if (NULL_CACHE_VALUE.equals(value)) {
                        return null;
                    }
                    if (value instanceof IpCityDTO) {
                        return (IpCityDTO) value;
                    }
                }
            }
            
            // 4. Query Local DB
            IpCityDTO result = localService.getCityByIp(ip);
            
            // 5. Fallback to Remote API
            if (Objects.isNull(result) || !StringUtils.hasText(result.getCity())) {
                try {
                    log.info("[IP-Fallback] Local lookup failed for IP={}, switching to Remote API", ip);
                    result = remoteService.getCityByIp(ip);
                } catch (Exception e) {
                    log.warn("[IP-Fallback] Remote query failed for IP={}", ip, e);
                }
            }

            // 6. Save result (包括负缓存)
            try {
                if (Objects.nonNull(result)) {
                    // Write L1
                    if (Objects.nonNull(cache)) cache.put(ip, result);
                    // Write L2
                    long redisTtl = ipCityConfig.getCache().getRedisTtlSeconds();
                    redisUtil.set(redisKey, JsonUtil.toJson(result), redisTtl, TimeUnit.SECONDS);
                } else {
                    // 穿透保护：写入负缓存
                    log.debug("[IP-Cache] Setting null-cache for IP={}", ip);
                    if (Objects.nonNull(cache)) cache.put(ip, NULL_CACHE_VALUE);
                    redisUtil.set(redisKey, NULL_CACHE_VALUE, NULL_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                log.warn("Failed to update cache for ip {}", ip, e);
            }

            return result;
        } finally {
            lock.unlock();
            // 清理锁映射（避免内存泄漏）
            // 注意：这里有一个竞态条件，但影响很小（最多多持有一个锁对象）
            lockMap.remove(ip, lock);
        }
    }
}
