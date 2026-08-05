package com.sparta.auth_service.application.exception;

/** IP sign-in rate limit 초과 — 429 AUTH_RATE_LIMITED */
public class LoginRateLimitedException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "로그인 요청이 많습니다. 잠시 후 다시 시도해 주세요.";

    private final long retryAfterSeconds;

    public LoginRateLimitedException(long retryAfterSeconds) {
        super(DEFAULT_MESSAGE);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
