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
     * Default query type: amap or geolite2
     */
    private String defaultType;

    /**
     * Amap API configuration
     */
    private Amap amap = new Amap();

    /**
     * GeoLite2 configuration
     */
    private Geolite2 geolite2 = new Geolite2();

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
}
