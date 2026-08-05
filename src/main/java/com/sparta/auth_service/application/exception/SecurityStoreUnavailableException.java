package com.sparta.auth_service.application.exception;

/**
 * Redis security store(login fail/lock, refresh, active access, blacklist) 장애.
 * fail-closed — HTTP 503 AUTH_SECURITY_STORE_UNAVAILABLE.
 */
public class SecurityStoreUnavailableException extends RuntimeException {

    public SecurityStoreUnavailableException(Throwable cause) {
        super(cause);
    }
}
