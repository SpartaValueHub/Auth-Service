package com.sparta.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthSignInRequestDto {

    private final String loginId;
    private final String password;
}
