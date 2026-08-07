package com.sparta.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthSignUpResumeResultDto {
    private final String authUuid;
    private final String signupCompletionToken;
}
