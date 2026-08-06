package com.sparta.auth_service.adaptor.in.web.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * IP 기반 sign-in 단기 rate limit — loginId fail count와 독립.
 */
@Getter
@Validated
@ConfigurationProperties(prefix = "auth.login-rate-limit")
public class LoginRateLimitProperties {

    private final boolean enabled;

    /** 윈도우 내 허용 요청 수 (초과 시 block) */
    @Min(1)
    private final int maxAttempts;

    /** 카운터 윈도우(초) */
    @Min(1)
    private final int windowSeconds;

    /** 초과 시 차단 지속(초) */
    @Min(1)
    private final int blockSeconds;

    public LoginRateLimitProperties(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("20") @Min(1) int maxAttempts,
            @DefaultValue("60") @Min(1) int windowSeconds,
            @DefaultValue("60") @Min(1) int blockSeconds
    ) {
        this.enabled = enabled;
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
        this.blockSeconds = blockSeconds;
    }
}
