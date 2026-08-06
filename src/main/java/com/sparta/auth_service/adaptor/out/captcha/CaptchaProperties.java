package com.sparta.auth_service.adaptor.out.captcha;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Google reCAPTCHA v2 siteverify 연동 설정 */
@Getter
@Validated
@ConfigurationProperties(prefix = "captcha")
public class CaptchaProperties {

    private final boolean enabled;

    @Min(1)
    private final int connectTimeoutMillis;

    @Min(1)
    private final int readTimeoutMillis;

    @Valid
    private final Recaptcha recaptcha;

    public CaptchaProperties(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("2000") @Min(1) int connectTimeoutMillis,
            @DefaultValue("3000") @Min(1) int readTimeoutMillis,
            @Valid Recaptcha recaptcha
    ) {
        this.enabled = enabled;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.recaptcha = recaptcha != null ? recaptcha : new Recaptcha("", "localhost,127.0.0.1", 120);
    }

    public Set<String> normalizedAllowedHostnames() {
        if (recaptcha == null || !StringUtils.hasText(recaptcha.getAllowedHostnames())) {
            return Set.of();
        }
        return Arrays.stream(recaptcha.getAllowedHostnames().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(hostname -> hostname.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Getter
    @Validated
    public static class Recaptcha {

        private final String secretKey;

        /** siteverify 응답 hostname 허용 목록 — 쉼표 구분, exact match(소문자 정규화). 비어 있으면 모두 거부 */
        private final String allowedHostnames;

        /** challenge_ts 최대 허용 경과(초) */
        @Min(1)
        private final int challengeMaxAgeSeconds;

        public Recaptcha(
                @DefaultValue("") String secretKey,
                @DefaultValue("localhost,127.0.0.1") String allowedHostnames,
                @DefaultValue("120") @Min(1) int challengeMaxAgeSeconds
        ) {
            this.secretKey = secretKey;
            this.allowedHostnames = allowedHostnames;
            this.challengeMaxAgeSeconds = challengeMaxAgeSeconds;
        }
    }
}
