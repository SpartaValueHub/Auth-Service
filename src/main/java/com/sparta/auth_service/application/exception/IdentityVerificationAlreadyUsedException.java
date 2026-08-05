package com.sparta.auth_service.application.exception;

/** SUCCESS 본인인증에 memberUuid 이미 연결 — requestToken 재사용 가입 차단 */
public class IdentityVerificationAlreadyUsedException extends RuntimeException {

    public IdentityVerificationAlreadyUsedException(String message) {
        super(message);
    }

    public String getCode() {
        return "IDENTITY_VERIFICATION_ALREADY_USED";
    }
}
