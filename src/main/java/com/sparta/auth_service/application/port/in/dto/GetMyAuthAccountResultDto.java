package com.sparta.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class GetMyAuthAccountResultDto {

    // 외부 식별자 (JWT sub / auth_uuid)
    private final String authUuid;
    // 로그인 아이디
    private final String loginId;
    // 이메일
    private final String email;
    // 휴대폰 번호
    private final String phoneNumber;
    // 가입일 (auth.created_at)
    private final Instant joinedAt;
}
