package com.example.sunxu_mall.service;

import com.example.sunxu_mall.dto.ip.IpCityDTO;

/**
 * IP城市查询服务接口
 *
 * @author sunxu
 */
public interface IpCityService {
    /**
     * 根据IP地址获取城市信息
     *
     * @param ip IP地址
     * @return IP城市查询结果
     */
    IpCityDTO getCityByIp(String ip);

}