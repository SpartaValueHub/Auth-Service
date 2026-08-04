package com.sparta.auth_service.application.port.out.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * PortOne 본인인증 조회 결과 — Application·sign-up·prefill 전용.
 * identity_verifications 테이블에는 status·purpose만 저장, PII는 영구 보관하지 않음.
 */
@Getter
@Builder
public class ExternalIdentityVerificationDto {

    private final String requestToken;
    private final String portOneStatus;
    /** PortOne CI — auth.identity_key 저장·중복 가입 검증에만 사용 */
    private final String identityKey;
    private final String memberName;
    private final String phoneNumber;
    private final LocalDate birthdayDate;
}
