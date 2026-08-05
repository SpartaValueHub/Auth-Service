package com.sparta.auth_service.adaptor.in.web.config;

import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/** Origin 문자열 파싱·정규화 — scheme+host+effective port exact match */
final class AuthOriginNormalizer {

    static final Set<String> LOCAL_DEV_HOSTS = Set.of("localhost", "127.0.0.1");

    private AuthOriginNormalizer() {
    }

    static String normalize(String raw) {
        return parse(raw).normalized();
    }

    static ParsedOrigin parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("Origin must not be blank");
        }
        String trimmed = raw.trim();
        if (trimmed.contains("*")) {
            throw new IllegalArgumentException("Origin must not contain wildcard");
        }

        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Origin is not a valid absolute URI", ex);
        }

        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("Origin must be an absolute URI");
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("Origin scheme must be http or https");
        }

        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            throw new IllegalArgumentException("Origin must include a host");
        }

        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Origin must not include user info");
        }

        String rawPath = uri.getRawPath();
        if (StringUtils.hasText(rawPath) && !"/".equals(rawPath)) {
            throw new IllegalArgumentException("Origin must not include a path");
        }

        if (uri.getRawQuery() != null) {
            throw new IllegalArgumentException("Origin must not include a query");
        }

        if (uri.getRawFragment() != null) {
            throw new IllegalArgumentException("Origin must not include a fragment");
        }

        String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        int effectivePort = uri.getPort();
        if (effectivePort == -1) {
            effectivePort = "https".equals(normalizedScheme) ? 443 : 80;
        }

        boolean defaultPort = ("https".equals(normalizedScheme) && effectivePort == 443)
                || ("http".equals(normalizedScheme) && effectivePort == 80);

        String normalized = defaultPort
                ? normalizedScheme + "://" + normalizedHost
                : normalizedScheme + "://" + normalizedHost + ":" + effectivePort;

        return new ParsedOrigin(normalized, normalizedHost);
    }

    record ParsedOrigin(String normalized, String host) {
    }
}
