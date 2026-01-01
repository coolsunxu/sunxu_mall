package com.example.sunxu_mall.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * IP City Query Configuration Properties
 *
 * @author sunxu
 */
@Data
@Component
@ConfigurationProperties(prefix = "mall.mgt.ip-city")
public class IpCityConfig {

    /**
     * Amap API configuration
     */
    private Amap amap = new Amap();

    /**
     * GeoLite2 configuration
     */
    private Geolite2 geolite2 = new Geolite2();

    /**
     * Cache configuration
     */
    private Cache cache = new Cache();

    @Data
    public static class Amap {
        /**
         * API key
         */
        private String key;

        /**
         * API URL
         */
        private String url;
    }

    @Data
    public static class Geolite2 {
        /**
         * Local database path (e.g., classpath:geoip/GeoLite2-City.mmdb)
         */
        private String dbPath;

        /**
         * Account ID for MaxMind GeoLite2 API
         */
        private String accountId;

        /**
         * License key for MaxMind GeoLite2 API
         */
        private String licenseKey;

        /**
         * API base URL
         */
        private String url;
    }

    @Data
    public static class Cache {
        /**
         * L1 Caffeine cache TTL in seconds (default 3600)
         */
        private long caffeineTtlSeconds = 3600;

        /**
         * L1 Caffeine cache max size (default 10000)
         */
        private long caffeineMaxSize = 10000;

        /**
         * L2 Redis cache TTL in seconds (default 86400)
         */
        private long redisTtlSeconds = 86400;
    }
}
