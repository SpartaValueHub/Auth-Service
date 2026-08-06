package com.sparta.auth_service.adaptor.in.web.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/** refresh·logout Cookie 인증 CSRF 방어 — Origin 허용 목록 */
@Getter
@ConfigurationProperties(prefix = "auth.origin")
public class AuthOriginProperties {

    /** 쉼표 구분 Origin 목록 (예: http://localhost:3000) */
    private final String allowedOrigins;

    /** true면 Origin 헤더 필수, false면 허용 목록에 있을 때만 검증 */
    private final boolean requireOrigin;

    private final Set<String> normalizedAllowedOrigins;

    public AuthOriginProperties(
            @DefaultValue("http://localhost:3000,http://127.0.0.1:3000") String allowedOrigins,
            @DefaultValue("false") boolean requireOrigin
    ) {
        this.allowedOrigins = allowedOrigins;
        this.requireOrigin = requireOrigin;
        this.normalizedAllowedOrigins = parseAllowedOrigins(allowedOrigins);
    }

    public boolean isAllowed(String origin) {
        if (!StringUtils.hasText(origin)) {
            return !requireOrigin;
        }
        try {
            return normalizedAllowedOrigins.contains(AuthOriginNormalizer.normalize(origin));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    Set<String> normalizedAllowedOrigins() {
        return normalizedAllowedOrigins;
    }

    static Set<String> parseAllowedOrigins(String rawAllowedOrigins) {
        if (!StringUtils.hasText(rawAllowedOrigins)) {
            return Set.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String entry : rawAllowedOrigins.split(",")) {
            String trimmed = entry.trim();
            if (!StringUtils.hasText(trimmed)) {
                throw new IllegalStateException("auth.origin.allowed-origins must not contain blank entries");
            }
            String normalizedOrigin = AuthOriginNormalizer.normalize(trimmed);
            if (!normalized.add(normalizedOrigin)) {
                throw new IllegalStateException("auth.origin.allowed-origins must not contain duplicates");
            }
        }
        return Set.copyOf(normalized);
    }
}
