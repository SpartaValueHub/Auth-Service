package com.sparta.auth_service.adaptor.out.captcha;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaPropertiesTest {

    @Test
    void normalizesAllowedHostnamesWithTrimAndLowercase() {
        CaptchaProperties properties = new CaptchaProperties();
        properties.getRecaptcha().setAllowedHostnames(" localhost , 127.0.0.1 , ValueHub.Example.COM ");

        assertThat(properties.normalizedAllowedHostnames())
                .containsExactlyInAnyOrder("localhost", "127.0.0.1", "valuehub.example.com");
    }

    @Test
    void returnsEmptySetWhenAllowedHostnamesBlank() {
        CaptchaProperties properties = new CaptchaProperties();
        properties.getRecaptcha().setAllowedHostnames("   ");

        assertThat(properties.normalizedAllowedHostnames()).isEmpty();
    }

    @Test
    void returnsEmptySetWhenAllowedHostnamesNull() {
        CaptchaProperties properties = new CaptchaProperties();
        properties.getRecaptcha().setAllowedHostnames(null);

        assertThat(properties.normalizedAllowedHostnames()).isEmpty();
    }

    @Test
    void ignoresEmptySegments() {
        CaptchaProperties properties = new CaptchaProperties();
        properties.getRecaptcha().setAllowedHostnames("localhost,,127.0.0.1,");

        assertThat(properties.normalizedAllowedHostnames())
                .isEqualTo(Set.of("localhost", "127.0.0.1"));
    }
}
