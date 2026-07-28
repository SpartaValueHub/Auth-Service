package com.unionclass.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthSignInRequestDto {

    private final String logInId;
    private final String password;
}
