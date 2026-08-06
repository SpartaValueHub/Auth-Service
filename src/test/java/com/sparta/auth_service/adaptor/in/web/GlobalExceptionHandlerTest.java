package com.sparta.auth_service.adaptor.in.web;

import com.sparta.auth_service.adaptor.in.web.vo.ErrorResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.IdentityVerificationStatusRequestVo;
import com.sparta.auth_service.application.exception.AccountLockedException;
import com.sparta.auth_service.application.exception.CaptchaProviderUnavailableException;
import com.sparta.auth_service.application.exception.DuplicateResourceException;
import com.sparta.auth_service.application.exception.ExternalIdentityProviderUnavailableException;
import com.sparta.auth_service.application.exception.LoginRateLimitedException;
import com.sparta.auth_service.application.exception.SessionTerminatedException;
import com.sparta.auth_service.application.exception.SecurityStoreUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = GlobalExceptionHandlerTestSupport.handler();
    }

    private void stubRequestPath() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/sign-up");
    }

    @Test
    void mapsLoginRateLimitedTo429WithRetryAfter() {
        stubRequestPath();
        LoginRateLimitedException ex = new LoginRateLimitedException(60L);

        ResponseEntity<ErrorResponseVo> response = handler.handleLoginRateLimited(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("AUTH_RATE_LIMITED");
        assertThat(response.getBody().getMessage())
                .isEqualTo("로그인 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        assertThat(response.getBody().getRetryAfterSeconds()).isEqualTo(60L);
    }

    @Test
    void mapsAccountLockedTo423WithRetryAfter() {
        stubRequestPath();
        AccountLockedException ex = new AccountLockedException(
                "로그인 시도가 많아 2분간 로그인이 제한됩니다.",
                120L
        );

        ResponseEntity<ErrorResponseVo> response = handler.handleAccountLocked(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("120");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("AUTH_ACCOUNT_LOCKED");
        assertThat(response.getBody().getMessage()).isEqualTo("로그인 시도가 많아 2분간 로그인이 제한됩니다.");
        assertThat(response.getBody().getRetryAfterSeconds()).isEqualTo(120L);
    }

    @Test
    void mapsDuplicateResourceExceptionTo409() {
        stubRequestPath();
        DuplicateResourceException ex = new DuplicateResourceException(
                "AUTH_DUPLICATE_EMAIL",
                "이미 사용 중인 email입니다."
        );

        ResponseEntity<ErrorResponseVo> response = handler.handleDuplicateResource(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("AUTH_DUPLICATE_EMAIL");
        assertThat(response.getBody().getMessage()).isEqualTo("이미 사용 중인 email입니다.");
        assertThat(response.getBody().getMessage()).doesNotContain("uk_auth_email");
    }

    @Test
    void dataIntegrityViolationAlwaysReturns500WithoutDbDetails() {
        stubRequestPath();
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "Duplicate entry 'a@b.com' for key 'uk_auth_email'"
        );

        ResponseEntity<ErrorResponseVo> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("요청을 처리할 수 없습니다.");
        assertThat(response.getBody().getMessage()).doesNotContain("uk_auth_email");
        assertThat(response.getBody().getMessage()).doesNotContain("Duplicate entry");
    }

    @Test
    void notNullIntegrityViolationReturns500() {
        stubRequestPath();
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "Column 'email' cannot be null"
        );

        ResponseEntity<ErrorResponseVo> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getMessage()).doesNotContain("email");
        assertThat(response.getBody().getMessage()).doesNotContain("member_name");
    }

    @Test
    void mapsSecurityStoreUnavailableTo503WithRetryAfter() {
        stubRequestPath();
        SecurityStoreUnavailableException ex = new SecurityStoreUnavailableException(new RuntimeException("redis down"));

        ResponseEntity<ErrorResponseVo> response = handler.handleSecurityStoreUnavailable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("5");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("AUTH_SECURITY_STORE_UNAVAILABLE");
        assertThat(response.getBody().getMessage())
                .isEqualTo("인증 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.");
        assertThat(response.getBody().getMessage()).doesNotContain("redis");
        assertThat(response.getBody().getRetryAfterSeconds()).isEqualTo(5L);
    }

    @Test
    void mapsCaptchaProviderUnavailableTo503() {
        stubRequestPath();
        CaptchaProviderUnavailableException ex =
                new CaptchaProviderUnavailableException(new RuntimeException("timeout"));

        ResponseEntity<ErrorResponseVo> response = handler.handleCaptchaProviderUnavailable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("AUTH_CAPTCHA_PROVIDER_UNAVAILABLE");
    }

    @Test
    void mapsExternalIdentityProviderUnavailableTo503() {
        stubRequestPath();
        ExternalIdentityProviderUnavailableException ex =
                new ExternalIdentityProviderUnavailableException(new RuntimeException("5xx"));

        ResponseEntity<ErrorResponseVo> response = handler.handleExternalIdentityProviderUnavailable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("AUTH_IDENTITY_PROVIDER_UNAVAILABLE");
    }

    @Test
    void methodArgumentNotValidReturns400WithFieldErrorsWithoutSensitiveValues() {
        when(request.getRequestURI()).thenReturn("/api/v1/identity-verifications/status");

        org.springframework.validation.BeanPropertyBindingResult bindingResult =
                new org.springframework.validation.BeanPropertyBindingResult(
                        new IdentityVerificationStatusRequestVo(),
                        "identityVerificationStatusRequestVo"
                );
        bindingResult.rejectValue("requestToken", "NotBlank", "requestToken은 필수입니다.");

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponseVo> response = handler.handleMethodArgumentNotValid(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getFieldErrors()).hasSize(1);
        assertThat(response.getBody().getFieldErrors().get(0).getField()).isEqualTo("requestToken");
        assertThat(response.getBody().getMessage()).doesNotContain("identity-verification");
    }

    @Test
    void mapsSessionTerminatedTo401() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/refresh");
        SessionTerminatedException ex = new SessionTerminatedException(
                "다른 기기에서 로그인하여 현재 세션이 종료되었습니다."
        );

        ResponseEntity<ErrorResponseVo> response = handler.handleSessionTerminated(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("AUTH_SESSION_TERMINATED");
        assertThat(response.getBody().getMessage())
                .isEqualTo("다른 기기에서 로그인하여 현재 세션이 종료되었습니다.");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/auth/refresh");
    }
}
