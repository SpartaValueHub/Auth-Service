package com.sparta.auth_service.adaptor.in.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@Schema(description = "내 계정 정보 조회 응답")
public class AuthAccountResponseVo {

    @Schema(description = "인증 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String authUuid;

    @Schema(description = "로그인 아이디", example = "user01")
    private String logInId;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "휴대폰 번호", example = "01012345678")
    private String phoneNumber;

    @Schema(description = "가입일 (ISO-8601)", example = "2026-08-04T08:00:00Z")
    private Instant joinedAt;
}
