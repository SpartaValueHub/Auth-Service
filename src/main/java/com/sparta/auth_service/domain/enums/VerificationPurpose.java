package com.sparta.auth_service.domain.enums;

/** 본인인증 요청 목적 — purpose별 재사용·연결 정책은 Application에서 분기 */
public enum VerificationPurpose {
    SIGN_UP,
    SOCIAL_LINK,
    FIND_ID,
    RESET_PASSWORD
}
