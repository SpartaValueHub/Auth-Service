package com.sparta.auth_service.adaptor.in.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/** 로그인·refresh 공통 응답 — JWT는 HttpOnly Cookie, body는 세션 메타만 */
@Getter
@Builder
@Schema(description = "로그인 응답")
public class AuthSignInResponseVo {

    @Schema(description = "회원 UUID (authUuid)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String memberUuid;

    @Schema(description = "닉네임 (member-service 연동 전 memberName)", example = "홍길동")
    private String nickname;

    @Schema(description = "역할", example = "USER")
    private String role;
}
