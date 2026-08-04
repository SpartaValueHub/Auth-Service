package com.sparta.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

/** 로그인·refresh 결과 — JWT claim에는 authUuid만, nickname 미포함 */
@Getter
@Builder
public class AuthSignInResultDto {

    private final String accessToken;
    private final String refreshToken;
    private final String authUuid;
    private final String loginId;
    private final String memberName;
    private final String email;
}
