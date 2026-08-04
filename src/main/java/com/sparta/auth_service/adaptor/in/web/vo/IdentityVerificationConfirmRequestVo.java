package com.sparta.auth_service.adaptor.in.web.vo;

import com.sparta.auth_service.domain.enums.VerificationPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 본인인증 confirm — identityVerificationId는 PortOne requestToken과 동일 값 */
@Getter
@NoArgsConstructor
@Schema(description = "본인인증 확인 요청")
public class IdentityVerificationConfirmRequestVo {

    @Schema(description = "PortOne identityVerificationId (프론트 생성)", example = "identity-verification-001")
    private String identityVerificationId;

    @Schema(description = "본인인증 목적", example = "SIGN_UP")
    private VerificationPurpose purpose;
}
