package com.sparta.auth_service.application.exception;

/** 가입 CI와 탈퇴 본인인증 CI 불일치 — 403 AUTH_IDENTITY_MISMATCH */
public class AuthIdentityMismatchException extends RuntimeException {

    public AuthIdentityMismatchException(String message) {
        super(message);
    }

    public String getCode() {
        return "AUTH_IDENTITY_MISMATCH";
    }
}
