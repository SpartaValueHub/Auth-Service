package com.sparta.auth_service.application.service;

/** recordLoginFailure 결과 — signIn 예외 매핑용 */
public enum LoginFailureOutcome {
    NORMAL_FAILURE,
    CAPTCHA_REQUIRED,
    ACCOUNT_LOCKED
}
