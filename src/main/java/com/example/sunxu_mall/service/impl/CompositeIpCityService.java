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

import java.util.concurrent.TimeUnit;

/**
 * 复合IP查询服务 (多级缓存 + 离线库 + 降级)
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
        if (cache != null) {
            IpCityDTO cached = cache.get(ip, IpCityDTO.class);
            if (cached != null) {
                log.info("[IP-Cache] Hit L1 for IP={}", ip);
                return cached;
            }
        }

        // 2. Check L2 Cache (Redis)
        String redisKey = REDIS_PREFIX + ip;
        try {
            String redisJson = redisUtil.get(redisKey);
            if (StringUtils.hasText(redisJson)) {
                IpCityDTO cached = JsonUtil.fromJson(redisJson, IpCityDTO.class);
                if (cached != null) {
                    log.info("[IP-Cache] Hit L2 for IP={}", ip);
                    // Write back to L1
                    if (cache != null) cache.put(ip, cached);
                    return cached;
                }
            }
        } catch (Exception e) {
            log.warn("[IP-Cache] Redis error for IP={}", ip, e);
        }

        // 3. Query Local DB
        IpCityDTO result = localService.getCityByIp(ip);
        
        // 4. Fallback to Remote API
        if (result == null || !StringUtils.hasText(result.getCity())) {
            try {
                // 如果本地库返回了对象但城市为空（可能只查到国家），或者完全为null，尝试远程
                // 注意：如果 localService 返回了 "Internal" 类型，通常 result.getCity() 是 "Local"，不会进这里
                log.info("[IP-Fallback] Local lookup failed for IP={}, switching to Remote API", ip);
                result = remoteService.getCityByIp(ip);
            } catch (Exception e) {
                log.warn("[IP-Fallback] Remote query failed for IP={}", ip, e);
            }
        }

        // 5. Save result
        if (result != null) {
            try {
                // Write L1
                if (cache != null) cache.put(ip, result);
                // Write L2
                long redisTtl = ipCityConfig.getCache().getRedisTtlSeconds();
                redisUtil.set(redisKey, JsonUtil.toJson(result), redisTtl, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Failed to update cache for ip {}", ip, e);
            }
        }

        return result;
    }
}
