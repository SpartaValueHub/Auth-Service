package com.sparta.auth_service.application.exception;

/** 본인인증 미완료(VERIFIED 아님) — sign-up·confirm 전 상태 */
public class IdentityVerificationNotReadyException extends RuntimeException {

    public IdentityVerificationNotReadyException(String message) {
        super(message);
    }

    public String getCode() {
        return "IDENTITY_VERIFICATION_NOT_READY";
    }
}
