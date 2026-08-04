package com.sparta.auth_service.domain.enums;

/** 본인인증 상태 — SUCCESS·FAILED·CANCELED·EXPIRED는 종료(isTerminal), 재사용 불가 */
public enum VerificationStatus {
    REQUESTED,
    SUCCESS,
    FAILED,
    CANCELED,
    EXPIRED
}
