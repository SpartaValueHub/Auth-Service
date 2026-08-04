package com.sparta.auth_service.adaptor.in.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/** loginId·email 중복 확인 결과 — phone·CI는 별도 API 없음(sign-up 시 서버 검증) */
@Getter
@Builder
@Schema(description = "중복 확인 결과")
public class AuthAvailabilityResponseVo {

    @Schema(description = "사용 가능 여부")
    private final boolean available;
}
