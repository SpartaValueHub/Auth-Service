package com.sparta.auth_service.application.port.out;

/** 로그인 실패·잠금 상태 — Redis TTL 기반, DB 미저장 */
public interface LoginAttemptPort {

    int getFailCount(String loginId);

    int incrementFailCount(String loginId);

    boolean isLocked(String loginId);

    /** login:lock 키 남은 TTL(초). 잠금 없으면 0 */
    long getLockRemainingSeconds(String loginId);

    void lock(String loginId);

    void reset(String loginId);
}
