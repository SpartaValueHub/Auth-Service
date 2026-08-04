package com.sparta.auth_service.adaptor.in.web;

import com.sparta.auth_service.adaptor.in.web.vo.ErrorResponseVo;
import com.sparta.auth_service.adaptor.out.portone.PortOneApiException;
import com.sparta.auth_service.application.exception.AccountLockedException;
import com.sparta.auth_service.application.exception.DuplicateResourceException;
import com.sparta.auth_service.application.exception.IdentityVerificationAlreadyUsedException;
import com.sparta.auth_service.application.exception.IdentityVerificationFailedException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotFoundException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotReadyException;
import com.sparta.auth_service.application.exception.InvalidTokenException;
import com.sparta.auth_service.application.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Domain/Application 예외 → 표준 JSON ErrorResponse 변환.
 * 스택·SQL·내부 클래스명은 외부 노출하지 않음.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseVo> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseVo> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponseVo> handleAccountLocked(
            AccountLockedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(HttpStatus.FORBIDDEN, "AUTH_ACCOUNT_LOCKED", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseVo> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(error(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponseVo> handleInvalidToken(
            InvalidTokenException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(error(HttpStatus.UNAUTHORIZED, ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(IdentityVerificationNotFoundException.class)
    public ResponseEntity<ErrorResponseVo> handleIdentityVerificationNotFound(
            IdentityVerificationNotFoundException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(HttpStatus.NOT_FOUND, "IDENTITY_VERIFICATION_NOT_FOUND", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(IdentityVerificationFailedException.class)
    public ResponseEntity<ErrorResponseVo> handleIdentityVerificationFailed(
            IdentityVerificationFailedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error(HttpStatus.BAD_REQUEST, "IDENTITY_VERIFICATION_FAILED", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(IdentityVerificationNotReadyException.class)
    public ResponseEntity<ErrorResponseVo> handleIdentityVerificationNotReady(
            IdentityVerificationNotReadyException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(IdentityVerificationAlreadyUsedException.class)
    public ResponseEntity<ErrorResponseVo> handleIdentityVerificationAlreadyUsed(
            IdentityVerificationAlreadyUsedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(PortOneApiException.class)
    public ResponseEntity<ErrorResponseVo> handlePortOneApi(
            PortOneApiException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(error(HttpStatus.BAD_GATEWAY, "PORTONE_API_ERROR", ex.getMessage(), request.getRequestURI()));
    }

    private ErrorResponseVo error(HttpStatus status, String code, String message, String path) {
        return ErrorResponseVo.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .code(code)
                .message(message)
                .path(path)
                .build();
    }
}
