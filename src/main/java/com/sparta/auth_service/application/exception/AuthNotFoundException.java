package com.sparta.auth_service.application.exception;

/** auth 계정 없음 — 404 AUTH_NOT_FOUND */
public class AuthNotFoundException extends RuntimeException {

    public AuthNotFoundException(String message) {
        super(message);
    }

    public String getCode() {
        return "AUTH_NOT_FOUND";
    }
}
