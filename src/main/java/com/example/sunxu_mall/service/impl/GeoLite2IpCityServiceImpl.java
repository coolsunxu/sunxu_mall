package com.example.sunxu_mall.service.impl;

import com.example.sunxu_mall.config.props.IpCityConfig;
import com.example.sunxu_mall.dto.ip.GeoIpDTO;
import com.example.sunxu_mall.dto.ip.IpCityDTO;
import com.example.sunxu_mall.mapper.IpCityMapper;
import com.example.sunxu_mall.service.IpCityService;
import com.example.sunxu_mall.util.JsonUtil;
import com.example.sunxu_mall.errorcode.ErrorCode;
import com.example.sunxu_mall.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

/**
 * GeoLite2 IP City Query Implementation
 * Note: This implementation uses MaxMind GeoLite2 web services API
 * Using local database file is recommended for production environment to improve performance
 *
 * @author sunxu
 */
@Slf4j
@Service("geoLite2IpCityApiService")
@RequiredArgsConstructor
public class GeoLite2IpCityServiceImpl implements IpCityService {

    private final OkHttpClient okHttpClient;
    private final IpCityConfig ipCityProperties;
    private final IpCityMapper ipCityMapper;

    @Override
    public IpCityDTO getCityByIp(String ip) {
        // Parameter validation
        if (StringUtils.isBlank(ip)) {
            log.warn("IP address is empty, cannot query city information");
            return null;
        }

        return queryGeoLite2ByApi(ip);
    }

    /**
     * Query IP address via MaxMind GeoIP2 API
     * Note: This method is for testing only, local database is recommended for production
     *
     * @param ip IP address to query
     * @return IP city information
     */
    private IpCityDTO queryGeoLite2ByApi(String ip) {
        // Read configuration
        String accountId = ipCityProperties.getGeolite2().getAccountId();
        String licenseKey = ipCityProperties.getGeolite2().getLicenseKey();
        String baseUrl = ipCityProperties.getGeolite2().getUrl();

        // Validate configuration
        if (StringUtils.isBlank(accountId)) {
            log.warn("GeoLite2 account ID is not configured. Please configure mall.mgt.ip-city.geolite2.account-id");
            throw new BusinessException(ErrorCode.CONFIG_ERROR.getCode(), "GeoLite2 account ID is not configured");
        }

        if (StringUtils.isBlank(licenseKey)) {
            log.error("GeoLite2 license key is not configured. Please configure mall.mgt.ip-city.geolite2.license-key");
            throw new BusinessException(ErrorCode.CONFIG_ERROR.getCode(), "GeoLite2 license key is not configured");
        }

        // Build API URL
        String apiUrl = baseUrl + "/" + ip;

        // Create Basic Auth credentials
        String credentials = Credentials.basic(accountId, licenseKey);

        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(apiUrl), "Invalid API URL")
                .newBuilder();

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .addHeader("Authorization", credentials)
                .build();

        log.debug("Querying GeoLite2 IP geolocation: ip={}, url={}, accountId={}", ip, apiUrl, accountId);

        try (Response response = okHttpClient.newCall(request).execute()) {
            // Check response status
            if (!response.isSuccessful()) {
                log.warn("GeoLite2 API call failed, httpStatus={}, ip={}", response.code(), ip);
                return null;
            }

            // Check response body
            if (Objects.isNull(response.body())) {
                log.warn("GeoLite2 API returned empty response body, ip={}", ip);
                return null;
            }

            String responseBody = response.body().string();
            log.debug("GeoLite2 API response: {}", responseBody);

            // Parse response
            GeoIpDTO geoIpDTO = JsonUtil.fromJson(responseBody, GeoIpDTO.class);

            if (geoIpDTO == null) {
                log.warn("Failed to parse GeoLite2 response, ip={}", ip);
                return null;
            }

            // Convert DTO using MapStruct
            return ipCityMapper.geoLite2ToIpCity(geoIpDTO, ip);

        } catch (IOException e) {
            log.warn("Exception calling GeoLite2 API, ip={} ", ip, e);
            return null;
        }
    }
}