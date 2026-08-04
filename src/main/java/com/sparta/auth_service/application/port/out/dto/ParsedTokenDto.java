package com.sparta.auth_service.application.port.out.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/** JWT 파싱 결과 — tokenId(jti)는 Redis refresh·blacklist 키 */
@Getter
@Builder
public class ParsedTokenDto {

    private final String tokenId;
    private final String authUuid;
    private final String tokenType;
    private final Instant expiresAt;
}
