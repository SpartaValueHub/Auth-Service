package com.sparta.auth_service.adaptor.in.web.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthOriginNormalizerTest {

    @Test
    void normalizesSchemeHostAndDefaultPort() {
        assertThat(AuthOriginNormalizer.normalize("HTTPS://Example.COM:443"))
                .isEqualTo("https://example.com");
        assertThat(AuthOriginNormalizer.normalize("http://LOCALHOST:80"))
                .isEqualTo("http://localhost");
    }

    @Test
    void keepsExplicitNonDefaultPort() {
        assertThat(AuthOriginNormalizer.normalize("http://localhost:3000"))
                .isEqualTo("http://localhost:3000");
        assertThat(AuthOriginNormalizer.normalize("https://valuehub.example.com:8443"))
                .isEqualTo("https://valuehub.example.com:8443");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "*",
            "https://*.example.com",
            "ftp://example.com",
            "https://",
            "https:///path",
            "https://user:pass@example.com",
            "https://example.com/path",
            "https://example.com?x=1",
            "https://example.com#frag",
            "not-a-uri",
            "example.com"
    })
    void rejectsInvalidOrigin(String origin) {
        assertThatThrownBy(() -> AuthOriginNormalizer.normalize(origin))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "http://127.0.0.1,127.0.0.1",
            "http://localhost:3000,localhost"
    })
    void parsesHost(String origin, String expectedHost) {
        assertThat(AuthOriginNormalizer.parse(origin).host()).isEqualTo(expectedHost);
    }
}
