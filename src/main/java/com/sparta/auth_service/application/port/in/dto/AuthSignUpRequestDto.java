package com.sparta.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

/** sign-up Input DTO — 실명·전화·CI는 requestToken→PortOne 조회로만 확정, body에 포함하지 않음 */
@Getter
@Builder
public class AuthSignUpRequestDto {

    private final String requestToken;
    private final String loginId;
    private final String password;
    private final String email;
}
