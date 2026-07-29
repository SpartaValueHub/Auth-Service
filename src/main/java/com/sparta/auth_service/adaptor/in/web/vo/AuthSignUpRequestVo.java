package com.unionclass.auth_service.adaptor.in.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "회원가입 요청")
public class AuthSignUpRequestVo {

    @Schema(description = "로그인 아이디", example = "user01")
    private String logInId;

    @Schema(description = "비밀번호 (8자 이상)", example = "password1")
    private String password;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "휴대폰 번호", example = "01012345678")
    private String phone;
}
