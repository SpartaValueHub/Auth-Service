package com.sparta.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/** sign-up 결과 — memberName·birthday는 PortOne→auth 테이블 반영값 */
@Getter
@Builder
public class AuthSignUpResultDto {

    private final String signupCompletionToken;

    private final String authUuid;
    private final String loginId;
    private final String email;
    private final String memberName;
    private final LocalDate birthdayDate;
}
