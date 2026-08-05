package com.sparta.auth_service.application.exception;

/**
 * reCAPTCHA siteverify 제공자 장애(타임아웃·네트워크·5xx·응답 파싱 실패).
 * 사용자 검증 실패(success=false 등)와 구분 — HTTP 503 AUTH_CAPTCHA_PROVIDER_UNAVAILABLE.
 */
public class CaptchaProviderUnavailableException extends RuntimeException {

    public CaptchaProviderUnavailableException(Throwable cause) {
        super(cause);
    }
}
