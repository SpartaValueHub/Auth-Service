package com.sparta.auth_service.adaptor.out.captcha;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaPropertiesTest {

    @Test
    void normalizesAllowedHostnamesWithTrimAndLowercase() {
        CaptchaProperties properties = captchaPropertiesWithAllowedHostnames(
                " localhost , 127.0.0.1 , ValueHub.Example.COM "
        );

        assertThat(properties.normalizedAllowedHostnames())
                .containsExactlyInAnyOrder("localhost", "127.0.0.1", "valuehub.example.com");
    }

    @Test
    void returnsEmptySetWhenAllowedHostnamesBlank() {
        CaptchaProperties properties = captchaPropertiesWithAllowedHostnames("   ");

        assertThat(properties.normalizedAllowedHostnames()).isEmpty();
    }

    @Test
    void returnsEmptySetWhenAllowedHostnamesNull() {
        CaptchaProperties properties = new CaptchaProperties(
                true,
                2000,
                3000,
                new CaptchaProperties.Recaptcha("", null, 120)
        );

        assertThat(properties.normalizedAllowedHostnames()).isEmpty();
    }

    @Test
    void ignoresEmptySegments() {
        CaptchaProperties properties = captchaPropertiesWithAllowedHostnames("localhost,,127.0.0.1,");

        assertThat(properties.normalizedAllowedHostnames())
                .isEqualTo(Set.of("localhost", "127.0.0.1"));
    }

    private static CaptchaProperties captchaPropertiesWithAllowedHostnames(String allowedHostnames) {
        return new CaptchaProperties(
                true,
                2000,
                3000,
                new CaptchaProperties.Recaptcha("", allowedHostnames, 120)
        );
    }
}
