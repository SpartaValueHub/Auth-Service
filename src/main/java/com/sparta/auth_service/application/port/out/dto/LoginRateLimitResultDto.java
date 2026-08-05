package com.sparta.auth_service.application.port.out.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginRateLimitResultDto {

    private final boolean allowed;
    private final long retryAfterSeconds;

    public static LoginRateLimitResultDto allowed() {
        return LoginRateLimitResultDto.builder()
                .allowed(true)
                .retryAfterSeconds(0L)
                .build();
    }

    public static LoginRateLimitResultDto blocked(long retryAfterSeconds) {
        return LoginRateLimitResultDto.builder()
                .allowed(false)
                .retryAfterSeconds(retryAfterSeconds)
                .build();
    }
}
