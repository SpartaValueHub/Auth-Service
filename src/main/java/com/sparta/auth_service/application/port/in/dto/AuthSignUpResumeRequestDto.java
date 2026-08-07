package com.sparta.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthSignUpResumeRequestDto {
    private final String loginId;
    private final String password;
    private final String captchaToken;
    private final String clientIp;
}
