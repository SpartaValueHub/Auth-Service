package com.sparta.auth_service.adaptor.in.web.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AuthSignUpResumeRequestVo {
    private String logInId;
    private String password;
    private String captchaToken;
}
