package com.sparta.auth_service.adaptor.out.captcha;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Google reCAPTCHA v2 siteverify 연동 설정 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "captcha")
public class CaptchaProperties {

    private boolean enabled = true;

    @Min(1)
    private int connectTimeoutMillis = 2000;

    @Min(1)
    private int readTimeoutMillis = 3000;

    @Valid
    private Recaptcha recaptcha = new Recaptcha();

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
    @Setter
    public static class Recaptcha {

        private String secretKey = "";

        /** siteverify 응답 hostname 허용 목록 — 쉼표 구분, exact match(소문자 정규화). 비어 있으면 모두 거부 */
        private String allowedHostnames = "localhost,127.0.0.1";

        /** challenge_ts 최대 허용 경과(초) */
        @Min(1)
        private int challengeMaxAgeSeconds = 120;
    }
}
