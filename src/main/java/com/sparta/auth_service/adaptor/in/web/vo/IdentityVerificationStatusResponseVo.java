package com.sparta.auth_service.adaptor.in.web.vo;

import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.enums.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/** 본인인증 status HTTP 응답 — purpose·status만 포함 */
@Getter
@Builder
@Schema(description = "본인인증 상태 조회 결과")
public class IdentityVerificationStatusResponseVo {

    @Schema(description = "본인인증 목적")
    private final VerificationPurpose purpose;

    @Schema(description = "본인인증 상태")
    private final VerificationStatus status;
}
