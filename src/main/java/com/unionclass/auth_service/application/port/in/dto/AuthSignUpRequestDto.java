package com.unionclass.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthSignUpRequestDto {

    private final String logInId;
    private final String password;
    private final String email;
    private final String name;
    private final String phone;
}
