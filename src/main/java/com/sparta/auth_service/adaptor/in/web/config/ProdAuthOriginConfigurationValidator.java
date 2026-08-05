package com.sparta.auth_service.adaptor.in.web.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * prod 프로필 Origin 설정 fail-closed 검증.
 * require-origin=true, 비어 있지 않은 allowlist, localhost/127.0.0.1 거부.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
class ProdAuthOriginConfigurationValidator {

    private final AuthOriginProperties authOriginProperties;

    @PostConstruct
    void validateProdOriginConfiguration() {
        if (!authOriginProperties.isRequireOrigin()) {
            throw new IllegalStateException(
                    "prod profile requires auth.origin.require-origin=true");
        }

        if (!StringUtils.hasText(authOriginProperties.getAllowedOrigins())) {
            throw new IllegalStateException(
                    "prod profile requires auth.origin.allowed-origins (AUTH_ALLOWED_ORIGINS)");
        }

        if (authOriginProperties.normalizedAllowedOrigins().isEmpty()) {
            throw new IllegalStateException(
                    "prod profile requires at least one valid auth.origin.allowed-origins entry");
        }

        for (String origin : authOriginProperties.normalizedAllowedOrigins()) {
            AuthOriginNormalizer.ParsedOrigin parsed = AuthOriginNormalizer.parse(origin);
            if (AuthOriginNormalizer.LOCAL_DEV_HOSTS.contains(parsed.host())) {
                throw new IllegalStateException(
                        "prod profile must not allow localhost or 127.0.0.1 origins");
            }
        }
    }
}
