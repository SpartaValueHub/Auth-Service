package com.sparta.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

/** refresh token rotation — Redis jti 일치 검증 후 재발급 */
@Getter
@Builder
public class AuthRefreshRequestDto {

    private final String refreshToken;
}
