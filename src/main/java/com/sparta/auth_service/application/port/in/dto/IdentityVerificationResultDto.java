package com.sparta.auth_service.application.port.in.dto;

import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.enums.VerificationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/** confirm·status 응답 — memberName·phone·birthday는 prefill 전용, DB 미저장 */
@Getter
@Builder
public class IdentityVerificationResultDto {

    private final String requestToken;
    private final VerificationPurpose purpose;
    private final VerificationStatus status;
    private final String memberName;
    private final String phoneNumber;
    private final LocalDate birthdayDate;
}
