package com.sparta.auth_service.adaptor.in.web.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 의존성 장애 시 Retry-After(초) — GlobalExceptionHandler 503 응답 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "auth.dependency-failure")
public class DependencyFailureProperties {

    @Min(1)
    private int retryAfterSeconds = 5;
}
