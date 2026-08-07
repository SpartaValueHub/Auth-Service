package com.sparta.auth_service.adaptor.in.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthSignUpResumeResponseVo {
    private String authUuid;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String signupCompletionToken;
}
