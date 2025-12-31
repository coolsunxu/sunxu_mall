package com.example.sunxu_mall.mapper;

import com.example.sunxu_mall.dto.ip.AmapIpDTO;
import com.example.sunxu_mall.dto.ip.GeoIpDTO;
import com.example.sunxu_mall.dto.ip.IpCityDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * IP City Query DTO Converter
 *
 * @author sunxu
 */
@Mapper(componentModel = "spring")
public interface IpCityMapper {
    /**
     * Convert AmapIpDTO to IpCityDTO
     *
     * @param source Amap IP query result
     * @param ip Query IP address
     * @return Standard IP city query result
     */
    @Mapping(target = "ip", source = "ip")
    @Mapping(target = "province", expression = "java(source.getProvinceAsString())")
    @Mapping(target = "city", expression = "java(source.getCityAsString())")
    @Mapping(target = "district", ignore = true)
    @Mapping(target = "longitude", expression = "java(parseLongitude(source.getRectangle()))")
    @Mapping(target = "latitude", expression = "java(parseLatitude(source.getRectangle()))")
    @Mapping(target = "isp", ignore = true)
    IpCityDTO amapToIpCity(AmapIpDTO source, String ip);

    /**
     * Convert GeoIpDTO to IpCityDTO
     *
     * @param source GeoLite2 IP query result
     * @param ip Query IP address
     * @return Standard IP city query result
     */
    @Mapping(target = "ip", source = "ip")
    @Mapping(target = "province", expression = "java(source.getSubdivisions() != null && !source.getSubdivisions().isEmpty() ? source.getSubdivisions().get(0).getNames().getZhCn() : null)")
    @Mapping(target = "city", expression = "java(source.getCity() != null && source.getCity().getNames() != null ? source.getCity().getNames().getZhCn() : null)")
    @Mapping(target = "district", ignore = true)
    @Mapping(target = "longitude", expression = "java(source.getLocation() != null ? String.valueOf(source.getLocation().getLongitude()) : null)")
    @Mapping(target = "latitude", expression = "java(source.getLocation() != null ? String.valueOf(source.getLocation().getLatitude()) : null)")
    @Mapping(target = "isp", expression = "java(source.getTraits() != null ? source.getTraits().getAutonomousSystemOrganization() : null)")
    IpCityDTO geoLite2ToIpCity(GeoIpDTO source, String ip);

    /**
     * Parse longitude from Amap rectangle field
     * rectangle format: minLng,minLat;maxLng,maxLat
     */
    @Named("parseLongitude")
    default String parseLongitude(String rectangle) {
        if (rectangle == null || rectangle.trim().isEmpty()) {
            return "";
        }
        try {
            String[] points = rectangle.split(";");
            if (points.length >= 2) {
                String[] minPoint = points[0].split(",");
                String[] maxPoint = points[1].split(",");
                if (minPoint.length >= 2 && maxPoint.length >= 2) {
                    double minLng = Double.parseDouble(minPoint[0]);
                    double maxLng = Double.parseDouble(maxPoint[0]);
                    return String.valueOf((minLng + maxLng) / 2);
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return "";
    }

    /**
     * Parse latitude from Amap rectangle field
     * rectangle format: minLng,minLat;maxLng,maxLat
     */
    @Named("parseLatitude")
    default String parseLatitude(String rectangle) {
        if (rectangle == null || rectangle.trim().isEmpty()) {
            return "";
        }
        try {
            String[] points = rectangle.split(";");
            if (points.length >= 2) {
                String[] minPoint = points[0].split(",");
                String[] maxPoint = points[1].split(",");
                if (minPoint.length >= 2 && maxPoint.length >= 2) {
                    double minLat = Double.parseDouble(minPoint[1]);
                    double maxLat = Double.parseDouble(maxPoint[1]);
                    return String.valueOf((minLat + maxLat) / 2);
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return "";
    }
}