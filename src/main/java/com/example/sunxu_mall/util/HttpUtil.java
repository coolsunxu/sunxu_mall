package com.example.sunxu_mall.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * HTTP工具类
 * 用于处理HTTP请求相关的工具方法
 */
@Slf4j
public class HttpUtil {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final String COMMA = ",";

    /**
     * 获取客户端真实IP地址
     * <p>
     * 支持多种代理头：
     * - X-Forwarded-For: 标准的代理头，包含经过的所有代理服务器IP，第一个为真实客户端IP
     * - X-Real-IP: Nginx代理服务器常用的头
     * - Proxy-Client-IP: Apache代理服务器常用的头
     * - WL-Proxy-Client-IP: WebLogic代理服务器常用的头
     * - HTTP_CLIENT_IP: 某些代理服务器使用的头
     * - HTTP_X_FORWARDED_FOR: 某些旧版代理服务器使用的头
     * </p>
     *
     * @param request HttpServletRequest
     * @return 客户端真实IP地址，如果无法获取则返回"unknown"
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            log.warn("HttpServletRequest is null, cannot get client IP");
            return UNKNOWN;
        }

        String ip = null;

        // 1. 尝试从X-Forwarded-For头获取（最常用）
        ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            log.debug("Get IP from X-Forwarded-For header: {}", ip);
            return extractFirstIp(ip);
        }

        // 2. 尝试从X-Real-IP头获取（Nginx常用）
        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) {
            log.debug("Get IP from X-Real-IP header: {}", ip);
            return ip.trim();
        }

        // 3. 尝试从Proxy-Client-IP头获取（Apache常用）
        ip = request.getHeader("Proxy-Client-IP");
        if (isValidIp(ip)) {
            log.debug("Get IP from Proxy-Client-IP header: {}", ip);
            return ip.trim();
        }

        // 4. 尝试从WL-Proxy-Client-IP头获取（WebLogic常用）
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(ip)) {
            log.debug("Get IP from WL-Proxy-Client-IP header: {}", ip);
            return ip.trim();
        }

        // 5. 尝试从HTTP_CLIENT_IP头获取
        ip = request.getHeader("HTTP_CLIENT_IP");
        if (isValidIp(ip)) {
            log.debug("Get IP from HTTP_CLIENT_IP header: {}", ip);
            return ip.trim();
        }

        // 6. 尝试从HTTP_X_FORWARDED_FOR头获取
        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (isValidIp(ip)) {
            log.debug("Get IP from HTTP_X_FORWARDED_FOR header: {}", ip);
            return extractFirstIp(ip);
        }

        // 7. 如果以上代理头都没有获取到，使用request.getRemoteAddr()
        ip = request.getRemoteAddr();
        if (isValidIp(ip)) {
            log.debug("Get IP from request.getRemoteAddr(): {}", ip);
            // 处理本地IPv6地址
            if (LOCALHOST_IPV6.equals(ip)) {
                return LOCALHOST_IPV4;
            }
            return ip.trim();
        }

        log.warn("Cannot get valid client IP from request, return 'unknown'");
        return UNKNOWN;
    }

    /**
     * 验证IP地址是否有效
     *
     * @param ip 待验证的IP地址字符串
     * @return true if valid, false otherwise
     */
    private static boolean isValidIp(String ip) {
        return StringUtils.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip);
    }

    /**
     * 从可能包含多个IP的字符串中提取第一个有效的IP地址
     * <p>
     * X-Forwarded-For头可能包含多个IP，格式：client, proxy1, proxy2, ...
     * 第一个IP是客户端真实IP，后续是经过的代理服务器IP
     * </p>
     *
     * @param ipListString 包含多个IP的字符串，以逗号分隔
     * @return 第一个有效的IP地址
     */
    private static String extractFirstIp(String ipListString) {
        if (StringUtils.isBlank(ipListString)) {
            return UNKNOWN;
        }

        // 如果只有一个IP，直接返回
        if (!ipListString.contains(COMMA)) {
            return ipListString.trim();
        }

        // 如果有多个IP，返回第一个
        String[] ipArray = ipListString.split(COMMA);
        for (String ip : ipArray) {
            if (isValidIp(ip)) {
                return ip.trim();
            }
        }

        return UNKNOWN;
    }

    /**
     * 判断IP地址是否为内网IP
     * <p>
     * 内网IP范围：
     * - 10.0.0.0 ~ 10.255.255.255
     * - 172.16.0.0 ~ 172.31.255.255
     * - 192.168.0.0 ~ 192.168.255.255
     * - 127.0.0.0 ~ 127.255.255.255 (回环地址)
     * </p>
     *
     * @param ip IP地址
     * @return true if internal IP, false otherwise
     */
    public static boolean isInternalIp(String ip) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }

        // 处理本地地址
        if (LOCALHOST_IPV4.equals(ip) || LOCALHOST_IPV6.equals(ip)) {
            return true;
        }

        try {
            InetAddress addr = InetAddress.getByName(ip);
            if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
                return true;
            }

            // 判断是否属于私有IP地址
            return addr.isSiteLocalAddress();
        } catch (UnknownHostException e) {
            log.warn("Cannot parse IP address: {}, error: {}", ip, e.getMessage());
            return false;
        }
    }

    /**
     * 获取客户端真实IP地址（排除内网IP）
     * <p>
     * 如果获取到的IP是内网IP或回环地址，则返回unknown
     * </p>
     *
     * @param request HttpServletRequest
     * @return 客户端公网IP地址，如果是内网IP则返回"unknown"
     */
    public static String getClientPublicIp(HttpServletRequest request) {
        String ip = getClientIp(request);

        if (isInternalIp(ip)) {
            log.debug("Client IP {} is internal IP, return 'unknown'", ip);
            return UNKNOWN;
        }

        return ip;
    }

    /**
     * 获取请求的完整URL
     *
     * @param request HttpServletRequest
     * @return 完整的请求URL，包括协议、域名、端口、路径和查询参数
     */
    public static String getFullRequestUrl(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        StringBuffer requestUrl = request.getRequestURL();
        String queryString = request.getQueryString();

        if (StringUtils.isNotBlank(queryString)) {
            requestUrl.append("?").append(queryString);
        }

        return requestUrl.toString();
    }

    /**
     * 获取请求的来源URL（Referer）
     *
     * @param request HttpServletRequest
     * @return Referer URL，如果不存在返回空字符串
     */
    public static String getReferer(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        String referer = request.getHeader("Referer");
        return StringUtils.defaultString(referer, "");
    }

    /**
     * 获取用户的User-Agent
     *
     * @param request HttpServletRequest
     * @return User-Agent字符串，如果不存在返回空字符串
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        String userAgent = request.getHeader("User-Agent");
        return StringUtils.defaultString(userAgent, "");
    }

    /**
     * 判断请求是否为AJAX请求
     *
     * @param request HttpServletRequest
     * @return true if AJAX request, false otherwise
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }
}
