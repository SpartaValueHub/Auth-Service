package com.sparta.auth_service.adaptor.in.web.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** 의존성 장애 시 Retry-After(초) — GlobalExceptionHandler 503 응답 */
@Getter
@Validated
@ConfigurationProperties(prefix = "auth.dependency-failure")
public class DependencyFailureProperties {

    @Min(1)
    private final int retryAfterSeconds;

    public DependencyFailureProperties(@DefaultValue("5") @Min(1) int retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
