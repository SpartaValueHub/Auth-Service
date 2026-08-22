package com.sparta.auth_service.application.port.out;

/** 계정 단위 세션 무효화 — 활성 access blacklist + access/refresh Redis 삭제 */
public interface SessionInvalidationPort {

    void revokeAllSessions(String authUuid);
}
