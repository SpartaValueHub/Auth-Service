package com.sparta.auth_service.adaptor.in.web;

import com.sparta.auth_service.adaptor.in.web.config.DependencyFailureProperties;
import com.sparta.auth_service.adaptor.in.web.vo.ErrorResponseVo;
import com.sparta.auth_service.application.exception.AccountLockedException;
import com.sparta.auth_service.application.exception.AuthIdentityMismatchException;
import com.sparta.auth_service.application.exception.AuthNotFoundException;
import com.sparta.auth_service.application.exception.CaptchaInvalidException;
import com.sparta.auth_service.application.exception.CaptchaProviderUnavailableException;
import com.sparta.auth_service.application.exception.CaptchaRequiredException;
import com.sparta.auth_service.application.exception.DuplicateResourceException;
import com.sparta.auth_service.application.exception.ExternalIdentityProviderUnavailableException;
import com.sparta.auth_service.application.exception.ForbiddenOriginException;
import com.sparta.auth_service.application.exception.IdentityVerificationAlreadyUsedException;
import com.sparta.auth_service.application.exception.IdentityVerificationFailedException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotFoundException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotReadyException;
import com.sparta.auth_service.application.exception.InvalidTokenException;
import com.sparta.auth_service.application.exception.LoginRateLimitedException;
import com.sparta.auth_service.application.exception.MemberNotActiveException;
import com.sparta.auth_service.application.exception.SecurityStoreUnavailableException;
import com.sparta.auth_service.application.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Domain/Application 예외 → 표준 JSON ErrorResponse 변환.
 * 스택·SQL·내부 클래스명·Redis host·외부 URL·token·secret은 외부 노출하지 않음.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String DEPENDENCY_UNAVAILABLE_MESSAGE =
            "인증 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.";

    private final DependencyFailureProperties dependencyFailureProperties;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseVo> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ErrorResponseVo.FieldErrorVo> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ErrorResponseVo.FieldErrorVo.builder()
                        .field(fieldError.getField())
                        .code(fieldError.getCode())
                        .message(fieldError.getDefaultMessage())
                        .build())
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseVo.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .code("VALIDATION_ERROR")
                        .message("요청 값이 올바르지 않습니다.")
                        .path(request.getRequestURI())
                        .fieldErrors(fieldErrors)
                        .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseVo> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ForbiddenOriginException.class)
    public ResponseEntity<ErrorResponseVo> handleForbiddenOrigin(
            ForbiddenOriginException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN_ORIGIN", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseVo> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseVo> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled data integrity violation at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_ERROR",
                        "요청을 처리할 수 없습니다.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(SecurityStoreUnavailableException.class)
    public ResponseEntity<ErrorResponseVo> handleSecurityStoreUnavailable(
            SecurityStoreUnavailableException ex,
            HttpServletRequest request
    ) {
        log.warn("security store unavailable at {} exception={}",
                request.getRequestURI(), ex.getClass().getSimpleName());
        log.debug("security store unavailable detail", ex.getCause());
        return dependencyUnavailable(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_SECURITY_STORE_UNAVAILABLE", request);
    }

    @ExceptionHandler(ExternalIdentityProviderUnavailableException.class)
    public ResponseEntity<ErrorResponseVo> handleExternalIdentityProviderUnavailable(
            ExternalIdentityProviderUnavailableException ex,
            HttpServletRequest request
    ) {
        log.warn("identity provider unavailable at {} exception={}",
                request.getRequestURI(), ex.getClass().getSimpleName());
        log.debug("identity provider unavailable detail", ex.getCause());
        return dependencyUnavailable(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_IDENTITY_PROVIDER_UNAVAILABLE", request);
    }

    @ExceptionHandler(CaptchaProviderUnavailableException.class)
    public ResponseEntity<ErrorResponseVo> handleCaptchaProviderUnavailable(
            CaptchaProviderUnavailableException ex,
            HttpServletRequest request
    ) {
        log.warn("captcha provider unavailable at {} exception={}",
                request.getRequestURI(), ex.getClass().getSimpleName());
        log.debug("captcha provider unavailable detail", ex.getCause());
        return dependencyUnavailable(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_CAPTCHA_PROVIDER_UNAVAILABLE", request);
    }

    @ExceptionHandler(LoginRateLimitedException.class)
    public ResponseEntity<ErrorResponseVo> handleLoginRateLimited(
            LoginRateLimitedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(error(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "AUTH_RATE_LIMITED",
                        ex.getMessage(),
                        request.getRequestURI(),
                        ex.getRetryAfterSeconds()
                ));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponseVo> handleAccountLocked(
            AccountLockedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.LOCKED)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(error(
                        HttpStatus.LOCKED,
                        "AUTH_ACCOUNT_LOCKED",
                        ex.getMessage(),
                        request.getRequestURI(),
                        ex.getRetryAfterSeconds()
                ));
    }

    @ExceptionHandler(CaptchaRequiredException.class)
    public ResponseEntity<ErrorResponseVo> handleCaptchaRequired(
            CaptchaRequiredException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(HttpStatus.FORBIDDEN, "AUTH_CAPTCHA_REQUIRED", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(CaptchaInvalidException.class)
    public ResponseEntity<ErrorResponseVo> handleCaptchaInvalid(
            CaptchaInvalidException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error(HttpStatus.BAD_REQUEST, "AUTH_CAPTCHA_INVALID", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MemberNotActiveException.class)
    public ResponseEntity<ErrorResponseVo> handleMemberNotActive(
            MemberNotActiveException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(HttpStatus.FORBIDDEN, "AUTH_MEMBER_NOT_ACTIVE", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseVo> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(error(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AuthNotFoundException.class)
    public ResponseEntity<ErrorResponseVo> handleAuthNotFound(
            AuthNotFoundException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AuthIdentityMismatchException.class)
    public ResponseEntity<ErrorResponseVo> handleAuthIdentityMismatch(
            AuthIdentityMismatchException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(HttpStatus.FORBIDDEN, ex.getCode(), ex.getMessage(), request.getRequestURI()));
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

    private ResponseEntity<ErrorResponseVo> dependencyUnavailable(
            HttpStatus status,
            String code,
            HttpServletRequest request
    ) {
        long retryAfterSeconds = dependencyFailureProperties.getRetryAfterSeconds();
        return ResponseEntity.status(status)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds))
                .body(error(
                        status,
                        code,
                        DEPENDENCY_UNAVAILABLE_MESSAGE,
                        request.getRequestURI(),
                        retryAfterSeconds
                ));
    }

    private ErrorResponseVo error(HttpStatus status, String code, String message, String path) {
        return error(status, code, message, path, null);
    }

    private ErrorResponseVo error(
            HttpStatus status,
            String code,
            String message,
            String path,
            Long retryAfterSeconds
    ) {
        return ErrorResponseVo.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .code(code)
                .message(message)
                .path(path)
                .retryAfterSeconds(retryAfterSeconds)
                .build();
    }
}
