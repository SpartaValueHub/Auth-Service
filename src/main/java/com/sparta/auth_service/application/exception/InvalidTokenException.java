package com.sparta.auth_service.application.exception;

/** JWT 파싱·rotation·tokenType 불일치 — 401 INVALID_TOKEN */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public String getCode() {
        return "INVALID_TOKEN";
    }
}
