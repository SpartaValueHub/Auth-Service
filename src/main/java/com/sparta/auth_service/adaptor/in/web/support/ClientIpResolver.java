package com.sparta.auth_service.adaptor.in.web.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * 클라이언트 IP 추출 — {@code server.forward-headers-strategy=native} + Tomcat
 * {@code internal-proxies} 하에서 신뢰 프록시(게이트웨이) 뒤 {@link HttpServletRequest#getRemoteAddr()}만 사용.
 * X-Forwarded-For를 직접 파싱하지 않으며, DNS 조회({@code InetAddress.getByName})도 하지 않는다.
 */
@Component
public class ClientIpResolver {

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return "";
        }
        return normalizeIp(remoteAddr);
    }

    /**
     * 숫자 IPv4/IPv6 리터럴만 정규화. 호스트명·잘못된 값은 빈 문자열(안전 fallback).
     * {@link InetAddress#getByAddress(byte[])}만 사용 — DNS 조회 없음.
     */
    static String normalizeIp(String ip) {
        String trimmed = ip.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        int zoneIndex = trimmed.indexOf('%');
        if (zoneIndex >= 0) {
            trimmed = trimmed.substring(0, zoneIndex);
        }

        byte[] addressBytes = parseNumericIpBytes(trimmed);
        if (addressBytes == null) {
            return "";
        }

        try {
            InetAddress address = InetAddress.getByAddress(addressBytes);
            String hostAddress = address.getHostAddress();
            if (address instanceof Inet6Address) {
                return hostAddress.toLowerCase(Locale.ROOT);
            }
            return hostAddress;
        } catch (UnknownHostException ex) {
            return "";
        }
    }

    private static byte[] parseNumericIpBytes(String value) {
        if (value.indexOf(':') >= 0) {
            return parseIpv6Bytes(value);
        }
        if (value.indexOf('.') >= 0) {
            return parseIpv4Bytes(value);
        }
        return null;
    }

    private static byte[] parseIpv4Bytes(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            String part = parts[i];
            if (part.isEmpty() || part.length() > 3) {
                return null;
            }
            if (part.length() > 1 && part.startsWith("0")) {
                return null;
            }
            int octet;
            try {
                octet = Integer.parseInt(part);
            } catch (NumberFormatException ex) {
                return null;
            }
            if (octet < 0 || octet > 255) {
                return null;
            }
            bytes[i] = (byte) octet;
        }
        return bytes;
    }

    private static byte[] parseIpv6Bytes(String value) {
        if (value.contains(".")) {
            return parseEmbeddedIpv4Ipv6Bytes(value);
        }

        int compressionIndex = value.indexOf("::");
        String[] hextets;
        if (compressionIndex >= 0) {
            if (value.indexOf("::", compressionIndex + 2) >= 0) {
                return null;
            }
            String before = value.substring(0, compressionIndex);
            String after = value.substring(compressionIndex + 2);
            String[] beforeParts = before.isEmpty() ? new String[0] : before.split(":");
            String[] afterParts = after.isEmpty() ? new String[0] : after.split(":");
            if (beforeParts.length + afterParts.length >= 8) {
                return null;
            }
            int missing = 8 - beforeParts.length - afterParts.length;
            hextets = new String[8];
            System.arraycopy(beforeParts, 0, hextets, 0, beforeParts.length);
            for (int i = beforeParts.length; i < beforeParts.length + missing; i++) {
                hextets[i] = "0";
            }
            System.arraycopy(afterParts, 0, hextets, beforeParts.length + missing, afterParts.length);
        } else {
            hextets = value.split(":");
            if (hextets.length != 8) {
                return null;
            }
        }

        byte[] bytes = new byte[16];
        for (int i = 0; i < 8; i++) {
            String hextet = hextets[i];
            if (hextet.isEmpty() || hextet.length() > 4) {
                return null;
            }
            int parsed;
            try {
                parsed = Integer.parseInt(hextet, 16);
            } catch (NumberFormatException ex) {
                return null;
            }
            if (parsed < 0 || parsed > 0xFFFF) {
                return null;
            }
            bytes[i * 2] = (byte) (parsed >> 8);
            bytes[i * 2 + 1] = (byte) parsed;
        }
        return bytes;
    }

    private static byte[] parseEmbeddedIpv4Ipv6Bytes(String value) {
        int lastColon = value.lastIndexOf(':');
        if (lastColon < 0) {
            return null;
        }
        byte[] ipv4 = parseIpv4Bytes(value.substring(lastColon + 1));
        if (ipv4 == null) {
            return null;
        }
        String ipv6Prefix = value.substring(0, lastColon);
        byte[] ipv6 = parseIpv6Bytes(ipv6Prefix + ":0:0");
        if (ipv6 == null) {
            return null;
        }
        ipv6[12] = ipv4[0];
        ipv6[13] = ipv4[1];
        ipv6[14] = ipv4[2];
        ipv6[15] = ipv4[3];
        return ipv6;
    }
}
