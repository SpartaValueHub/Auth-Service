package com.unionclass.auth_service.adaptor.in.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "로그인 응답")
public class AuthSignInResponseVo {

    @Schema(description = "액세스 토큰")
    private String accessToken;

    @Schema(description = "리프레시 토큰")
    private String refreshToken;

    @Schema(description = "사용자 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String userId;

    @Schema(description = "로그인 아이디", example = "user01")
    private String logInId;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;
}
