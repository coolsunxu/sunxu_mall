package com.example.sunxu_mall.dto.ip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IP城市查询结果DTO
 *
 * @author sunxu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpCityDTO {
    /**
     * IP地址
     */
    private String ip;

    /**
     * 国家
     */
    private String country;
    
    /**
     * 省份
     */
    private String province;
    
    /**
     * 城市
     */
    private String city;
    
    /**
     * 区县
     */
    private String district;
    
    /**
     * 经度
     */
    private String longitude;
    
    /**
     * 纬度
     */
    private String latitude;
    
    /**
     * 运营商
     */
    private String isp;
    
    /**
     * 查询方式 (amap or geolite2)
     */
    private String queryType;

}