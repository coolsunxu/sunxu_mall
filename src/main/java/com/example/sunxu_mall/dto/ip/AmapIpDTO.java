package com.example.sunxu_mall.dto.ip;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Amap IP Response DTO
 * Note: province and city fields may be returned as arrays or strings by the API
 *
 * @author sunxu
 * @version 1.0
 * @date 2025/12/31 18:47
 */
@Data
public class AmapIpDTO {
    /**
     * Status code: 1 indicates success
     */
    private String status;

    /**
     * Status message: OK indicates success
     */
    private String info;

    /**
     * Info code: 10000 indicates success
     */
    @JsonProperty("infocode")
    private String infoCode;

    /**
     * Province name - may be array or string
     */
    private Object province;

    /**
     * City name - may be array or string
     */
    private Object city;

    /**
     * Administrative division code
     */
    @JsonProperty("adcode")
    private String adCode;

    /**
     * Geographic rectangle bounds: format "minLng,minLat;maxLng,maxLat"
     */
    private String rectangle;

    /**
     * Get province as string
     * @return Province name or empty string if not available
     */
    public String getProvinceAsString() {
        if (province == null) {
            return "";
        }
        if (province instanceof List) {
            List<?> list = (List<?>) province;
            return list.isEmpty() ? "" : list.get(0).toString();
        }
        return province.toString();
    }

    /**
     * Get city as string
     * @return City name or empty string if not available
     */
    public String getCityAsString() {
        if (city == null) {
            return "";
        }
        if (city instanceof List) {
            List<?> list = (List<?>) city;
            return list.isEmpty() ? "" : list.get(0).toString();
        }
        return city.toString();
    }
}
