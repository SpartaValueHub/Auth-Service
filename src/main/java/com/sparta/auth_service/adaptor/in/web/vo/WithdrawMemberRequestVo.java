package com.sparta.auth_service.adaptor.in.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "회원 탈퇴 요청")
public class WithdrawMemberRequestVo {

    @Schema(description = "탈퇴용 본인인증 requestToken (purpose=WITHDRAWAL confirm SUCCESS)", example = "identity-verification-withdraw-001")
    private String requestToken;
}
