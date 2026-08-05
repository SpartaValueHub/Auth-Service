package com.sparta.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

/** 로그인·refresh 결과 — JWT는 HttpOnly Cookie, body에는 세션 메타만 */
@Getter
@Builder
public class AuthSignInResultDto {

    private final String accessToken;
    private final String refreshToken;
    private final String authUuid;
    private final String loginId;
    private final String memberName;
    private final String email;
    /** member-service 연동 전 기본 역할 */
    private final String role;
}
