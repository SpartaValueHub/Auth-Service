package com.sparta.auth_service.application.exception;

/**
 * PortOne 등 외부 본인인증 API 장애(타임아웃·네트워크·5xx·429·응답 파싱 실패).
 * fail-closed — HTTP 503 AUTH_IDENTITY_PROVIDER_UNAVAILABLE.
 */
public class ExternalIdentityProviderUnavailableException extends RuntimeException {

    public ExternalIdentityProviderUnavailableException(Throwable cause) {
        super(cause);
    }
}
