package com.sparta.auth_service.application.port.in.dto;

/** refresh token rotation — Redis jti 일치 검증 후 재발급 */
public record AuthRefreshRequestDto(
        String refreshToken,
        /** 이중 로그인 판별용 — 브라우저가 함께 전송하는 access Cookie (선택) */
        String accessToken
) {
}
