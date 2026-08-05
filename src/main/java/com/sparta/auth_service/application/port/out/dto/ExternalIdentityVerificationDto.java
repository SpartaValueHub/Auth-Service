package com.sparta.auth_service.application.port.out.dto;

import com.sparta.auth_service.domain.enums.Gender;
import com.sparta.auth_service.domain.enums.VerificationMethod;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * PortOne 본인인증 조회 결과 — Application·sign-up·prefill 전용.
 * CI는 confirm 시 identity_verifications에 암호화 저장.
 */
@Getter
@Builder
public class ExternalIdentityVerificationDto {

    private final String requestToken;
    private final String portOneStatus;
    /** PortOne CI — confirm/sign-up 검증에 사용, 평문은 영구 저장하지 않음 */
    private final String identityKey;
    private final VerificationMethod verificationMethod;
    private final String memberName;
    private final String phoneNumber;
    private final LocalDate birthdayDate;
    private final Gender gender;
}
