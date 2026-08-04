package com.sparta.auth_service.adaptor.in.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 회원가입 HTTP 요청 — 실명·전화·CI는 body에 포함하지 않음(PortOne requestToken으로 서버 확정) */
@Getter
@NoArgsConstructor
@Schema(description = "회원가입 요청")
public class AuthSignUpRequestVo {

    @Schema(description = "본인인증 requestToken (PortOne identityVerificationId)", example = "identity-verification-001")
    private String requestToken;

    @Schema(description = "로그인 아이디", example = "user01")
    private String logInId;

    @Schema(description = "비밀번호 (8~20자)", example = "Password1!")
    private String password;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;
}
