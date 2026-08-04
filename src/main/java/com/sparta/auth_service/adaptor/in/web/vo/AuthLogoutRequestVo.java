package com.sparta.auth_service.adaptor.in.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 로그아웃 HTTP 요청 — refresh(Redis 삭제) + access(blacklist) 각각 필요 */
@Getter
@NoArgsConstructor
@Schema(description = "로그아웃 요청")
public class AuthLogoutRequestVo {

    @Schema(description = "Access Token")
    private String accessToken;

    @Schema(description = "Refresh Token")
    private String refreshToken;
}
