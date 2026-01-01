package com.example.sunxu_mall.service.impl;

import com.example.sunxu_mall.config.props.IpCityConfig;
import com.example.sunxu_mall.dto.ip.GeoIpDTO;
import com.example.sunxu_mall.dto.ip.IpCityDTO;
import com.example.sunxu_mall.mapper.IpCityMapper;
import com.example.sunxu_mall.service.IpCityService;
import com.example.sunxu_mall.util.JsonUtil;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Subdivision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

/**
 * 本地 GeoLite2 离线库查询服务
 */
@Slf4j
@Service("localGeoLite2IpCityService")
public class LocalGeoLite2IpCityServiceImpl implements IpCityService {

    private final IpCityConfig ipCityConfig;
    private final ResourceLoader resourceLoader;
    private DatabaseReader databaseReader;
    private final IpCityMapper ipCityMapper;

    public LocalGeoLite2IpCityServiceImpl(
            IpCityConfig ipCityConfig,
            ResourceLoader resourceLoader,
            IpCityMapper ipCityMapper
    ) {
        this.ipCityConfig = ipCityConfig;
        this.resourceLoader = resourceLoader;
        this.ipCityMapper = ipCityMapper;
    }

    @PostConstruct
    public void init() {
        String dbPath = ipCityConfig.getGeolite2().getDbPath();
        if (!StringUtils.hasText(dbPath)) {
            log.warn("GeoLite2 database path is not configured.");
            return;
        }
        
        try {
            Resource resource = resourceLoader.getResource(dbPath);
            if (resource.exists()) {
                InputStream inputStream = resource.getInputStream();
                // DatabaseReader 内部会缓存，适合高并发
                databaseReader = new DatabaseReader.Builder(inputStream).build();
                log.info("GeoLite2 database loaded successfully from: {}", dbPath);
            } else {
                log.warn("GeoLite2 database file not found at: {}", dbPath);
            }
        } catch (Exception e) {
            log.warn("Failed to load GeoLite2 database from {}: {}", dbPath, e.getMessage());
        }
    }
    
    @PreDestroy
    public void close() {
        if (databaseReader != null) {
            try {
                databaseReader.close();
            } catch (IOException e) {
                log.error("Error closing GeoLite2 database reader", e);
            }
        }
    }

    @Override
    public IpCityDTO getCityByIp(String ip) {
        // 特殊处理内网IP和本地IP
        if (isInternalIp(ip)) {
            log.debug("[IP-Local] IP={} is internal/local, skipping lookup", ip);
            return IpCityDTO.builder()
                    .ip(ip)
                    .country("Internal")
                    .province("Local")
                    .city("Local")
                    .queryType("internal")
                    .build();
        }

        if (databaseReader == null) {
            log.error("[IP-Error] GeoLite2 database not initialized, cannot query IP: {}", ip);
            return null;
        }
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = databaseReader.city(ipAddress);

            // Parse response
            GeoIpDTO geoIpDTO = JsonUtil.fromJson(JsonUtil.toJson(response), GeoIpDTO.class);

            if (geoIpDTO == null) {
                log.warn("Failed to parse GeoLite2 response, ip={}", ip);
                return null;
            }

            // Convert DTO using MapStruct
            return ipCityMapper.geoLite2ToIpCity(geoIpDTO, ip);

        } catch (com.maxmind.geoip2.exception.AddressNotFoundException e) {
            log.debug("[IP-Miss] IP={} not found in GeoLite2 database", ip);
            return null;
        } catch (Exception e) {
            log.warn("[IP-Error] GeoLite2 lookup error for IP={}: {}", ip, e.getMessage());
            return null;
        }
    }
    
    private boolean isInternalIp(String ip) {
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "localhost".equalsIgnoreCase(ip)
                || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.16.");
    }
    
    private String getChineseName(java.util.Map<String, String> names, String defaultName) {
        if (names == null) return defaultName;
        return names.getOrDefault("zh-CN", defaultName);
    }
}
