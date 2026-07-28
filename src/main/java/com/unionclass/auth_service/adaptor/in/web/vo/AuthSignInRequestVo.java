package com.unionclass.auth_service.adaptor.in.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "로그인 요청")
public class AuthSignInRequestVo {

    @Schema(description = "로그인 아이디", example = "user01")
    private String logInId;

    @Schema(description = "비밀번호", example = "password1")
    private String password;
}
