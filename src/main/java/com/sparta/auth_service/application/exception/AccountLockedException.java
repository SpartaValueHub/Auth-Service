package com.sparta.auth_service.application.exception;

/** loginFailCount 상한 도달 — Domain.isLocked와 연동, 403 AUTH_ACCOUNT_LOCKED */
public class AccountLockedException extends RuntimeException {

    public AccountLockedException(String message) {
        super(message);
    }
}
