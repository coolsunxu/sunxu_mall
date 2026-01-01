package com.example.sunxu_mall.service.impl;

import com.example.sunxu_mall.config.props.IpCityConfig;
import com.example.sunxu_mall.dto.ip.AmapIpDTO;
import com.example.sunxu_mall.dto.ip.IpCityDTO;
import com.example.sunxu_mall.errorcode.ErrorCode;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.mapper.IpCityMapper;
import com.example.sunxu_mall.service.IpCityService;
import com.example.sunxu_mall.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

/**
 * Amap API IP City Query Implementation
 * Efficient IP address to city information query based on OkHttp and MapStruct
 *
 * @author sunxu
 */
@Slf4j
@Service("amapIpCityService")
@RequiredArgsConstructor
public class AmapIpCityServiceImpl implements IpCityService {

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

        // Build request URL
        String baseUrl = ipCityProperties.getAmap().getUrl();
        String apiKey = ipCityProperties.getAmap().getKey();

        if (StringUtils.isBlank(apiKey)) {
            log.warn("Amap API key is not configured or using default value. Please configure mall.mgt.ip-city.amap.key");
            throw new BusinessException(ErrorCode.CONFIG_ERROR.getCode(), "Amap API key is not configured");
        }

        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(baseUrl), "Invalid API URL")
                .newBuilder();
        urlBuilder.addQueryParameter("key", apiKey);
        urlBuilder.addQueryParameter("ip", ip);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();

        log.info("Querying Amap IP geolocation: ip={}, url={}", ip, urlBuilder.build());

        try (Response response = okHttpClient.newCall(request).execute()) {
            // Check response status
            if (!response.isSuccessful()) {
                log.warn("Amap API call failed, httpStatus={}, ip={}", response.code(), ip);
                return null;
            }

            // Check response body
            if (Objects.isNull(response.body())) {
                log.warn("Amap API returned empty response body, ip={}", ip);
                return null;
            }

            String responseBody = response.body().string();
            log.debug("Amap API response: {}", responseBody);

            // Parse response
            AmapIpDTO amapIpDTO = JsonUtil.fromJson(responseBody, AmapIpDTO.class);

            // Check business status code
            if (amapIpDTO == null || !"1".equals(amapIpDTO.getStatus())) {
                log.warn("Amap API returned error, status={}, info={}, ip={}",
                        amapIpDTO != null ? amapIpDTO.getStatus() : "null",
                        amapIpDTO != null ? amapIpDTO.getInfo() : "null",
                        ip);
                return null;
            }

            // Convert DTO using MapStruct
            return ipCityMapper.amapToIpCity(amapIpDTO, ip);

        } catch (IOException e) {
            log.warn("Exception calling Amap API, ip={}", ip, e);
            return null;
        }
    }
}
