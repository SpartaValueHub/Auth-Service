package com.sparta.auth_service.domain.enums;

/** 회원 계정 상태 — 로그인 잠금(Redis)과 별개 */
public enum MemberStatus {
    ACTIVE,
    SUSPENDED,
    WITHDRAWN,
    DORMANT
}
