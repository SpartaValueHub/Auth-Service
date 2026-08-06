package com.sparta.auth_service.adaptor.in.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 본인인증 상태 조회 POST body — requestToken을 URL·query·cookie에 노출하지 않음 */
@Getter
@NoArgsConstructor
@Schema(description = "본인인증 상태 조회 요청")
public class IdentityVerificationStatusRequestVo {

    @NotBlank(message = "requestToken은 필수입니다.")
    @Size(max = 255, message = "requestToken은 255자 이하여야 합니다.")
    @Schema(description = "본인인증 요청 토큰 (PortOne identityVerificationId)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String requestToken;
}
