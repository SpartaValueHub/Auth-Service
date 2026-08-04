package com.sparta.auth_service.adaptor.in.web.vo;

import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.enums.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/** 본인인증 HTTP 응답 — memberName·phone·birthday는 prefill 전용, DB 이력에는 status만 저장 */
@Getter
@Builder
@Schema(description = "본인인증 결과")
public class IdentityVerificationResponseVo {

    @Schema(description = "본인인증 요청 토큰 (PortOne identityVerificationId)")
    private final String requestToken;

    @Schema(description = "본인인증 목적")
    private final VerificationPurpose purpose;

    @Schema(description = "본인인증 상태")
    private final VerificationStatus status;

    @Schema(description = "인증된 이름 (SUCCESS 시)")
    private final String memberName;

    @Schema(description = "인증된 휴대폰 번호 (SUCCESS 시)")
    private final String phoneNumber;

    @Schema(description = "인증된 생년월일 (SUCCESS 시)")
    private final LocalDate birthdayDate;
}
