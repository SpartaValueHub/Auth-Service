package com.sparta.auth_service.application.port.in.dto;

import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.enums.VerificationStatus;
import lombok.Builder;
import lombok.Getter;

/** status 조회 응답 — purpose·status만 포함, PII·requestToken 미포함 */
@Getter
@Builder
public class IdentityVerificationStatusResultDto {

    private final VerificationPurpose purpose;
    private final VerificationStatus status;
}
