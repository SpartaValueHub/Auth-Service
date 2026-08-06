package com.sparta.auth_service.application.exception;

/** 다른 기기 로그인 등으로 refresh jti가 덮어써진 세션 — 401 AUTH_SESSION_TERMINATED */
public class SessionTerminatedException extends RuntimeException {

    public SessionTerminatedException(String message) {
        super(message);
    }

    public String getCode() {
        return "AUTH_SESSION_TERMINATED";
    }
}
