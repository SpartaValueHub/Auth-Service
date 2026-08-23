package com.sparta.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class GetMemberJoinedAtResultDto {

    // 외부 공개 회원 UUID (authUuid 와 동일)
    private final String memberUuid;
    // 가입일 (auth.created_at)
    private final Instant joinedAt;
}
