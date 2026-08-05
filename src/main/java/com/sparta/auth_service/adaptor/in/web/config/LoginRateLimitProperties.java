package com.sparta.auth_service.adaptor.in.web.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * IP 기반 sign-in 단기 rate limit — loginId fail count와 독립.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "auth.login-rate-limit")
public class LoginRateLimitProperties {

    private boolean enabled = true;

    /** 윈도우 내 허용 요청 수 (초과 시 block) */
    @Min(1)
    private int maxAttempts = 20;

    /** 카운터 윈도우(초) */
    @Min(1)
    private int windowSeconds = 60;

    /** 초과 시 차단 지속(초) */
    @Min(1)
    private int blockSeconds = 60;
}
