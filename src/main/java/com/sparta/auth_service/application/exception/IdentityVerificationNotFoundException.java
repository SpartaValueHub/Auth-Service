package com.sparta.auth_service.application.exception;

/** requestToken 이력·PortOne 조회 결과 없음 — 404 IDENTITY_VERIFICATION_NOT_FOUND */
public class IdentityVerificationNotFoundException extends RuntimeException {

    public IdentityVerificationNotFoundException(String message) {
        super(message);
    }
}
