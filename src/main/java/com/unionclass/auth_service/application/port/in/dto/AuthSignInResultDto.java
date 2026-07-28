package com.unionclass.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthSignInResultDto {

    private final String accessToken;
    private final String refreshToken;
    private final String userId;
    private final String logInId;
    private final String name;
    private final String email;
}
