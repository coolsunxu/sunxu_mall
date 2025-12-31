package com.example.sunxu_mall.dto.ip;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/31 18:47
 * @description
 */

@Data
public class GeoIpDTO {
    /** 城市级信息 */
    private City city;

    /** 大洲信息 */
    private Continent continent;

    /** 国家/地区信息（IP 所属） */
    private Country country;

    /** 经纬度、时区等定位数据 */
    private Location location;

    /** 邮政编码 */
    private Postal postal;

    /** IP 注册国（可能与 country 不同，比如海外军事基地） */
    @JsonProperty("registered_country")
    private Country registeredCountry;

    /** 一级行政区划列表（例如省/州），最多两级 */
    private List<Subdivision> subdivisions;

    /** 网络层特征：IP、ASN、网段等 */
    private Traits traits;

    /* ==================== 嵌套对象 ==================== */

    @lombok.Data
    public static class City {
        /** GeoName.org 城市 ID */
        @JsonProperty("geoname_id")
        private Long geonameId;

        /** 多语言城市名称映射 */
        private Names names;
    }

    @lombok.Data
    public static class Continent {
        /** 大洲代码，如 NA、AS、EU */
        @JsonProperty("code")
        private String code;

        /** GeoName.org 大洲 ID */
        @JsonProperty("geoname_id")
        private Long geonameId;

        /** 多语言大洲名称 */
        private Names names;
    }

    @lombok.Data
    public static class Country {
        /** ISO-3166 两位国家代码，如 US、CN */
        @JsonProperty("iso_code")
        private String isoCode;

        /** GeoName.org 国家 ID */
        @JsonProperty("geoname_id")
        private Long geonameId;

        /** 多语言国家名称 */
        private Names names;
        
        /** 是否为欧盟国家 */
        @JsonProperty("is_in_european_union")
        private Boolean isInEuropeanUnion;
    }

    @lombok.Data
    public static class Location {
        /** 定位精度半径（公里），越小越准 */
        @JsonProperty("accuracy_radius")
        private Integer accuracyRadius;

        /** 纬度 */
        private Double latitude;

        /** 经度 */
        private Double longitude;

        /** 美国 DMA 代码（仅美加有用） */
        @JsonProperty("metro_code")
        private Integer metroCode;

        /** IANA 时区，如 America/Los_Angeles */
        @JsonProperty("time_zone")
        private String timeZone;
    }

    @lombok.Data
    public static class Postal {
        /** 邮政编码，如 90017 */
        private String code;
    }

    @lombok.Data
    public static class Subdivision {
        /** 一级行政区 ISO 代码，如 CN-BJ、US-CA */
        @JsonProperty("iso_code")
        private String isoCode;

        /** GeoName.org 行政区 ID */
        @JsonProperty("geoname_id")
        private Long geonameId;

        /** 多语言行政区名称 */
        private Names names;
    }

    @lombok.Data
    public static class Traits {
        /** 自治系统号（ASN） */
        @JsonProperty("autonomous_system_number")
        private Long autonomousSystemNumber;

        /** ASN 所属组织名称，如 IT7NET */
        @JsonProperty("autonomous_system_organization")
        private String autonomousSystemOrganization;

        /** 查询的 IP 地址 */
        @JsonProperty("ip_address")
        private String ipAddress;

        /** IP 所属 CIDR 网段，如 97.64.104.0/21 */
        private String network;
    }

    /** 多语言名称统一对象，字段即语言代码 */
    @lombok.Data
    public static class Names {
        /** 简体中文 */
        @JsonProperty("zh-CN")
        private String zhCn;

        /** 英语 */
        private String en;

        /** 法语 */
        private String fr;

        /** 德语 */
        private String de;

        /** 西班牙语 */
        private String es;

        /** 日语 */
        private String ja;

        /** 巴西葡萄牙语 */
        @JsonProperty("pt-BR")
        private String ptBr;

        /** 俄语 */
        private String ru;
    }
}
