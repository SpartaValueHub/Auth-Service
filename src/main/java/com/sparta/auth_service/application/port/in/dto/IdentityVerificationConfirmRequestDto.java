package com.sparta.auth_service.application.port.in.dto;

import com.sparta.auth_service.domain.enums.VerificationPurpose;
import lombok.Builder;
import lombok.Getter;

/** confirm 요청 — identityVerificationId는 PortOne requestToken과 동일 */
@Getter
@Builder
public class IdentityVerificationConfirmRequestDto {

    private final String identityVerificationId;
    private final VerificationPurpose purpose;
}
