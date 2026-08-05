package com.sparta.auth_service.application.exception;

/** captcha 토큰 검증 실패 — 400 AUTH_CAPTCHA_INVALID */
public class CaptchaInvalidException extends RuntimeException {

    public CaptchaInvalidException(String message) {
        super(message);
    }
}
