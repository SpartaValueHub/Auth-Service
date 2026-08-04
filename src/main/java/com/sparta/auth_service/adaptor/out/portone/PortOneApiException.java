package com.sparta.auth_service.adaptor.out.portone;

/** PortOne HTTP·파싱 실패 — GlobalExceptionHandler에서 502 PORTONE_API_ERROR로 변환 */
public class PortOneApiException extends RuntimeException {

    public PortOneApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
