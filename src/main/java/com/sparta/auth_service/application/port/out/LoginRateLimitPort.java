package com.sparta.auth_service.application.port.out;

import com.sparta.auth_service.application.port.out.dto.LoginRateLimitResultDto;

/** sign-in IP rate limit — Redis TTL·원자적 increment */
public interface LoginRateLimitPort {

    /**
     * sign-in 요청 1건을 기록하고 허용 여부를 반환한다.
     * Redis 장애 시 fail-open(allowed).
     */
    LoginRateLimitResultDto checkAndRecord(String clientIp);
}
