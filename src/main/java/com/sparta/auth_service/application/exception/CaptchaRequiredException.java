package com.sparta.auth_service.application.exception;

/** 로그인 실패 5회 이상 — captcha 필요, 403 AUTH_CAPTCHA_REQUIRED */
public class CaptchaRequiredException extends RuntimeException {

    public CaptchaRequiredException(String message) {
        super(message);
    }
}
