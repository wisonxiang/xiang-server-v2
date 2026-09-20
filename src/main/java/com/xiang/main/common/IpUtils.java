package com.xiang.main.common;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 取客户端真实 IP：经过 Nginx 等反向代理时，远端地址是代理的地址，
 * 因此优先取代理写入的 X-Forwarded-For（逗号分隔时第一个才是客户端），其次 X-Real-IP
 */
public final class IpUtils {

    private IpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
