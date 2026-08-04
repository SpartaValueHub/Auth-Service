package com.sparta.auth_service.application.exception;

/** PortOne VERIFIED이나 CI·고객정보 불완전 — sign-up·confirm 거부 */
public class IdentityVerificationFailedException extends RuntimeException {

    public IdentityVerificationFailedException(String message) {
        super(message);
    }
}
