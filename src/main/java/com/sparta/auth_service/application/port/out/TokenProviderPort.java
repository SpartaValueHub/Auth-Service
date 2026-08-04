package com.sparta.auth_service.application.port.out;

import com.sparta.auth_service.application.port.out.dto.ParsedTokenDto;

/** JWT RS256 발급·파싱 — Private Key는 Outbound Adapter만 보유 */
public interface TokenProviderPort {

    String createAccessToken(String authUuid);

    String createRefreshToken(String authUuid);

    ParsedTokenDto parseRefreshToken(String refreshToken);

    ParsedTokenDto parseAccessToken(String accessToken);
}
