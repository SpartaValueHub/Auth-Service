package com.sparta.auth_service.adaptor.in.web.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthOriginPropertiesTest {

    @Test
    void allowsNormalizedOriginMatch() {
        AuthOriginProperties properties = new AuthOriginProperties("http://localhost:3000", true);

        assertThat(properties.isAllowed("HTTP://LOCALHOST:3000")).isTrue();
        assertThat(properties.isAllowed("http://evil.example.com")).isFalse();
    }

    @Test
    void rejectsMissingOriginWhenRequired() {
        AuthOriginProperties properties = new AuthOriginProperties("http://localhost:3000", true);

        assertThat(properties.isAllowed(null)).isFalse();
        assertThat(properties.isAllowed("")).isFalse();
        assertThat(properties.isAllowed("   ")).isFalse();
    }

    @Test
    void allowsMissingOriginWhenNotRequired() {
        AuthOriginProperties properties = new AuthOriginProperties("http://localhost:3000", false);

        assertThat(properties.isAllowed(null)).isTrue();
    }

    @Test
    void rejectsMalformedRequestOrigin() {
        AuthOriginProperties properties = new AuthOriginProperties("http://localhost:3000", true);

        assertThat(properties.isAllowed("https://localhost:3000/evil")).isFalse();
    }

    @Test
    void rejectsDuplicateConfiguredOrigins() {
        assertThatThrownBy(() -> new AuthOriginProperties("http://localhost:3000,http://localhost:3000", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicates");
    }

    @Test
    void rejectsBlankConfiguredEntries() {
        assertThatThrownBy(() -> new AuthOriginProperties("http://localhost:3000,,http://127.0.0.1:3000", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blank entries");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://*.example.com",
            "https://example.com/path"
    })
    void rejectsInvalidConfiguredOrigins(String allowedOrigins) {
        assertThatThrownBy(() -> new AuthOriginProperties(allowedOrigins, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
