package com.unionclass.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthSignUpResultDto {

    private final String userId;
    private final String logInId;
    private final String email;
    private final String name;
}
