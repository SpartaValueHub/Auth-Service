package com.sparta.auth_service.adaptor.in.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@Schema(description = "회원 가입일 조회 응답")
public class MemberJoinedAtResponseVo {

    @Schema(description = "회원 UUID (authUuid)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String memberUuid;

    @Schema(description = "가입일 (ISO-8601 Instant, auth.created_at)", example = "2026-08-04T08:00:00Z")
    private Instant joinedAt;
}
