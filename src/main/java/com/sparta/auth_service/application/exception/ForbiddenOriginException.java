package com.sparta.auth_service.application.exception;

/** refresh·logout Origin 검증 실패 — 403 AUTH_FORBIDDEN_ORIGIN */
public class ForbiddenOriginException extends RuntimeException {

    public ForbiddenOriginException(String message) {
        super(message);
    }
}
