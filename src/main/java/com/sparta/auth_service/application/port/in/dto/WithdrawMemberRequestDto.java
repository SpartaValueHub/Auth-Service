package com.sparta.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WithdrawMemberRequestDto {

    // Gateway X-Member-Uuid (= authUuid)
    private final String authUuid;
    // 탈퇴용 본인인증 confirm SUCCESS requestToken
    private final String requestToken;
}
