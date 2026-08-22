package com.sparta.auth_service.application.port.out;

/** 가입 CI 선점 — UNIQUE(ci_hash)로 race 방지. 탈퇴 시 release로 재가입 허용 */
public interface SignupIdentityClaimPort {

    void claim(String ciHash, String authUuid);

    // UNIQUE 인덱스 1회 조회 — 현재 유효(미탈퇴) CI 선점 여부
    boolean existsByCiHash(String ciHash);

    // auth_uuid UNIQUE 기준 DELETE — 없으면 no-op
    void releaseByAuthUuid(String authUuid);
}
