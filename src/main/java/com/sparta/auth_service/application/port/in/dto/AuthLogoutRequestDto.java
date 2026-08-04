package com.sparta.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

/** logout — refresh(Redis 삭제) + access(blacklist TTL=잔여 만료) */
@Getter
@Builder
public class AuthLogoutRequestDto {

    private final String accessToken;
    private final String refreshToken;
}
