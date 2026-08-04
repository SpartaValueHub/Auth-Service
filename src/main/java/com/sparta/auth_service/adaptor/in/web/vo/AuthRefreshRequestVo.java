package com.sparta.auth_service.adaptor.in.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** refresh token rotation — 기존 refresh jti는 Redis에서 무효화 후 재발급 */
@Getter
@NoArgsConstructor
@Schema(description = "토큰 갱신 요청")
public class AuthRefreshRequestVo {

    @Schema(description = "Refresh Token")
    private String refreshToken;
}
