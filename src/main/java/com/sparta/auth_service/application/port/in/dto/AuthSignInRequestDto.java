package com.sparta.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthSignInRequestDto {

    private final String loginId;
    private final String password;
    private final String captchaToken;
    /** Web 계층에서 추출한 클라이언트 IP (IPv4/IPv6) */
    private final String clientIp;
}
