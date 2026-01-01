package com.example.sunxu_mall.validator;

import com.alibaba.fastjson.JSON;
import com.example.sunxu_mall.dto.ip.IpCityDTO;
import com.example.sunxu_mall.entity.common.CommonTaskEntity;
import com.example.sunxu_mall.entity.sys.web.UserWebEntity;
import com.example.sunxu_mall.event.UserLoginEvent;
import com.example.sunxu_mall.mapper.common.CommonTaskEntityMapper;
import com.example.sunxu_mall.mapper.sys.UserWebEntityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 登录验证器（静态工具类）
 */
@Slf4j
public class LoginValidator {

    /**
     * 异地登录验证
     *
     * @param event           登录事件
     * @param ipCityDTO       当前IP对应的城市信息
     */
    public static boolean checkGeoLocation(UserLoginEvent event, IpCityDTO ipCityDTO) {
        if (Objects.isNull(ipCityDTO) || !StringUtils.hasText(ipCityDTO.getCity())) {
            return false;
        }

        if (Objects.isNull(event) || !StringUtils.hasText(event.getCity())) {
            return false;
        }

        return !event.getCity().equals(ipCityDTO.getCity());

    }
}
