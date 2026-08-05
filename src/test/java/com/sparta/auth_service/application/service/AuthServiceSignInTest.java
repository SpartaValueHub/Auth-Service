package com.sparta.auth_service.application.service;

import com.sparta.auth_service.adaptor.in.web.config.LoginAttemptProperties;
import com.sparta.auth_service.adaptor.out.security.JwtProperties;
import com.sparta.auth_service.application.exception.AccountLockedException;
import com.sparta.auth_service.application.exception.CaptchaInvalidException;
import com.sparta.auth_service.application.exception.CaptchaProviderUnavailableException;
import com.sparta.auth_service.application.exception.CaptchaRequiredException;
import com.sparta.auth_service.application.exception.SecurityStoreUnavailableException;
import com.sparta.auth_service.application.exception.LoginRateLimitedException;
import com.sparta.auth_service.application.exception.UnauthorizedException;
import com.sparta.auth_service.application.port.in.dto.AuthSignInRequestDto;
import com.sparta.auth_service.application.port.out.AccessTokenBlacklistPort;
import com.sparta.auth_service.application.port.out.ActiveAccessTokenPort;
import com.sparta.auth_service.application.port.out.AuthRepositoryPort;
import com.sparta.auth_service.application.port.out.CaptchaVerificationPort;
import com.sparta.auth_service.application.port.out.FetchIdentityVerificationPort;
import com.sparta.auth_service.application.port.out.IdentityVerificationRepositoryPort;
import com.sparta.auth_service.application.port.out.LoginAttemptPort;
import com.sparta.auth_service.application.port.out.LoginRateLimitPort;
import com.sparta.auth_service.application.port.out.dto.LoginRateLimitResultDto;
import com.sparta.auth_service.application.port.out.PasswordEncoderPort;
import com.sparta.auth_service.application.port.out.RefreshTokenPort;
import com.sparta.auth_service.application.port.out.IdentityKeyHashPort;
import com.sparta.auth_service.application.port.out.TokenProviderPort;
import com.sparta.auth_service.application.port.out.dto.ParsedTokenDto;
import com.sparta.auth_service.domain.enums.Gender;
import com.sparta.auth_service.domain.enums.MemberStatus;
import com.sparta.auth_service.domain.model.AuthDomain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceSignInTest {

    @Mock
    private AuthRepositoryPort authRepositoryPort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @Mock
    private TokenProviderPort tokenProviderPort;

    @Mock
    private RefreshTokenPort refreshTokenPort;

    @Mock
    private ActiveAccessTokenPort activeAccessTokenPort;

    @Mock
    private AccessTokenBlacklistPort accessTokenBlacklistPort;

    @Mock
    private IdentityVerificationRepositoryPort identityVerificationRepositoryPort;

    @Mock
    private FetchIdentityVerificationPort fetchIdentityVerificationPort;

    @Mock
    private LoginAttemptPort loginAttemptPort;

    @Mock
    private LoginRateLimitPort loginRateLimitPort;

    @Mock
    private CaptchaVerificationPort captchaVerificationPort;

    @Mock
    private IdentityKeyHashPort identityKeyHashPort;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private LoginAttemptProperties loginAttemptProperties;

    @Mock
    private Clock clock;

    @InjectMocks
    private AuthService authService;

    private static final Instant FIXED_NOW = Instant.parse("2025-06-01T12:00:00Z");

    private static final String PASSWORD_HASH = "$2a$10$encodedhashvalueplaceholder";

    @BeforeEach
    void setUpPolicy() {
        when(clock.instant()).thenReturn(FIXED_NOW);
        when(loginAttemptProperties.getCaptchaThreshold()).thenReturn(5);
        when(loginAttemptProperties.getLockThreshold()).thenReturn(6);
        when(loginAttemptProperties.getLockDurationMinutes()).thenReturn(1);
        when(loginRateLimitPort.checkAndRecord(any())).thenReturn(LoginRateLimitResultDto.allowed());
    }

    @Test
    void signIn_throwsRateLimitedBeforeLoginAttemptChecks() {
        when(loginRateLimitPort.checkAndRecord("203.0.113.10"))
                .thenReturn(LoginRateLimitResultDto.blocked(60L));

        assertThatThrownBy(() -> authService.signIn(signInRequestWithIp("user01", "Password1!", "203.0.113.10")))
                .isInstanceOf(LoginRateLimitedException.class)
                .satisfies(thrown -> {
                    LoginRateLimitedException ex = (LoginRateLimitedException) thrown;
                    assertThat(ex.getRetryAfterSeconds()).isEqualTo(60L);
                });

        verify(loginAttemptPort, never()).isLocked(any());
        verify(loginAttemptPort, never()).getFailCount(any());
        verify(authRepositoryPort, never()).findByLoginId(any());
        verify(passwordEncoderPort, never()).matches(any(), any());
        verify(captchaVerificationPort, never()).verify(any());
    }

    @Test
    void signIn_rateLimitedDoesNotResetIpCounterOnWouldBeSuccess() {
        when(loginRateLimitPort.checkAndRecord("203.0.113.10"))
                .thenReturn(LoginRateLimitResultDto.blocked(30L));

        assertThatThrownBy(() -> authService.signIn(signInRequestWithIp("user01", "Password1!", "203.0.113.10")))
                .isInstanceOf(LoginRateLimitedException.class);

        verify(loginAttemptPort, never()).reset(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void signIn_returnsUnauthorizedForFailuresBelowCaptchaThreshold(int priorFailCount) {
        AuthDomain auth = activeAuth();
        when(authRepositoryPort.findByLoginId("user01")).thenReturn(Optional.of(auth));
        when(loginAttemptPort.isLocked("user01")).thenReturn(false);
        when(loginAttemptPort.getFailCount("user01")).thenReturn(priorFailCount);
        when(passwordEncoderPort.matches("wrong", PASSWORD_HASH)).thenReturn(false);
        when(loginAttemptPort.incrementFailCount("user01")).thenReturn(priorFailCount + 1);

        assertThatThrownBy(() -> authService.signIn(signInRequest("user01", "wrong")))
                .isInstanceOf(UnauthorizedException.class);

        verify(captchaVerificationPort, never()).verify(any());
        verify(loginAttemptPort, never()).lock("user01");
    }

    @Test
    void signIn_returnsCaptchaRequiredOnFifthFailure() {
        AuthDomain auth = activeAuth();
        when(authRepositoryPort.findByLoginId("user01")).thenReturn(Optional.of(auth));
        when(loginAttemptPort.isLocked("user01")).thenReturn(false);
        when(loginAttemptPort.getFailCount("user01")).thenReturn(4);
        when(passwordEncoderPort.matches("wrong", PASSWORD_HASH)).thenReturn(false);
        when(loginAttemptPort.incrementFailCount("user01")).thenReturn(5);

        assertThatThrownBy(() -> authService.signIn(signInRequest("user01", "wrong")))
                .isInstanceOf(CaptchaRequiredException.class);

        verify(captchaVerificationPort, never()).verify(any());
        verify(loginAttemptPort, never()).lock("user01");
    }

    @Test
    void signIn_requiresCaptchaWhenFailCountAlreadyAtThreshold() {
        when(loginAttemptPort.isLocked("user01")).thenReturn(false);
        when(loginAttemptPort.getFailCount("user01")).thenReturn(5);

        assertThatThrownBy(() -> authService.signIn(signInRequest("user01", "Password1!")))
                .isInstanceOf(CaptchaRequiredException.class);

        verify(authRepositoryPort, never()).findByLoginId(any());
        verify(passwordEncoderPort, never()).matches(any(), any());
        verify(loginAttemptPort, never()).incrementFailCount(any());
    }

    @Test
    void signIn_invalidCaptchaDoesNotIncrementFailCount() {
        when(loginAttemptPort.isLocked("user01")).thenReturn(false);
        when(loginAttemptPort.getFailCount("user01")).thenReturn(5);
        when(captchaVerificationPort.verify("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.signIn(signInRequestWithCaptcha("user01", "Password1!", "bad-token")))
                .isInstanceOf(CaptchaInvalidException.class);

        verify(loginAttemptPort, never()).incrementFailCount(any());
        verify(authRepositoryPort, never()).findByLoginId(any());
        verify(passwordEncoderPort, never()).matches(any(), any());
    }

    @Test
    void signIn_captchaProviderFailureDoesNotIncrementFailCountOrLock() {
        when(loginAttemptPort.isLocked("user01")).thenReturn(false);
        when(loginAttemptPort.getFailCount("user01")).thenReturn(5);
        when(captchaVerificationPort.verify("response-token"))
                .thenThrow(new CaptchaProviderUnavailableException(new RuntimeException("timeout")));

        assertThatThrownBy(() -> authService.signIn(signInRequestWithCaptcha("user01", "Password1!", "response-token")))
                .isInstanceOf(CaptchaProviderUnavailableException.class);

        verify(loginAttemptPort, never()).incrementFailCount(any());
        verify(loginAttemptPort, never()).lock(any());
        verify(authRepositoryPort, never()).findByLoginId(any());
        verify(passwordEncoderPort, never()).matches(any(), any());
    }

    @Test
    void signIn_redisLoginAttemptFailureReturns503BeforePasswordCheck() {
        when(loginAttemptPort.isLocked("user01"))
                .thenThrow(new SecurityStoreUnavailableException(new RuntimeException("redis down")));

        assertThatThrownBy(() -> authService.signIn(signInRequest("user01", "Password1!")))
                .isInstanceOf(SecurityStoreUnavailableException.class);

        verify(passwordEncoderPort, never()).matches(any(), any());
        verify(captchaVerificationPort, never()).verify(any());
    }

    @Test
    void signIn_captchaSiteverifyTimeoutDoesNotIncrementFailCountOrLock() {
        when(loginAttemptPort.isLocked("user01")).thenReturn(false);
        when(loginAttemptPort.getFailCount("user01")).thenReturn(5);
        when(captchaVerificationPort.verify("response-token"))
                .thenThrow(new CaptchaProviderUnavailableException(new RuntimeException("timeout")));

        assertThatThrownBy(() -> authService.signIn(signInRequestWithCaptcha("user01", "Password1!", "response-token")))
                .isInstanceOf(CaptchaProviderUnavailableException.class);

        verify(loginAttemptPort, never()).incrementFailCount(any());
        verify(loginAttemptPort, never()).lock(any());
        verify(authRepositoryPort, never()).findByLoginId(any());
        verify(passwordEncoderPort, never()).matches(any(), any());
    }

    @Test
    void signIn_captchaSiteverifyParseFailureDoesNotIncrementFailCountOrLock() {
        when(loginAttemptPort.isLocked("user01")).thenReturn(false);
        when(loginAttemptPort.getFailCount("user01")).thenReturn(5);
        when(captchaVerificationPort.verify("response-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.signIn(signInRequestWithCaptcha("user01", "Password1!", "response-token")))
                .isInstanceOf(CaptchaInvalidException.class);

        verify(loginAttemptPort, never()).incrementFailCount(any());
        verify(loginAttemptPort, never()).lock(any());
        verify(authRepositoryPort, never()).findByLoginId(any());
        verify(passwordEncoderPort, never()).matches(any(), any());
    }

    @Test
    void signIn_missingCaptchaDoesNotVerifyPassword() {
        when(loginAttemptPort.isLocked("user01")).thenReturn(false);
        when(loginAttemptPort.getFailCount("user01")).thenReturn(5);

        assertThatThrownBy(() -> authService.signIn(signInRequest("user01", "Password1!")))
                .isInstanceOf(CaptchaRequiredException.class);

        verify(passwordEncoderPort, never()).matches(any(), any());
        verify(loginAttemptPort, never()).incrementFailCount(any());
    }

    @Test
    void signIn_locksAfterCaptchaPassAndWrongPassword() {
        AuthDomain auth = activeAuth();
        when(authRepositoryPort.findByLoginId("user01")).thenReturn(Optional.of(auth));
        when(loginAttemptPort.isLocked("user01")).thenReturn(false);
        when(loginAttemptPort.getFailCount("user01")).thenReturn(5);
        when(captchaVerificationPort.verify("valid-token")).thenReturn(true);
        when(passwordEncoderPort.matches("wrong", PASSWORD_HASH)).thenReturn(false);
        when(loginAttemptPort.incrementFailCount("user01")).thenReturn(6);

        assertThatThrownBy(() -> authService.signIn(signInRequestWithCaptcha("user01", "wrong", "valid-token")))
                .isInstanceOf(AccountLockedException.class)
                .satisfies(thrown -> {
                    AccountLockedException ex = (AccountLockedException) thrown;
                    assertThat(ex.getMessage()).isEqualTo("로그인 시도가 많아 1분간 로그인이 제한됩니다.");
                    assertThat(ex.getRetryAfterSeconds()).isEqualTo(60L);
                });

        verify(loginAttemptPort).lock("user01");
    }

    @Test
    void signIn_lockMessageUsesConfiguredLockDurationMinutes() {
        when(loginAttemptProperties.getLockDurationMinutes()).thenReturn(3);
        AuthDomain auth = activeAuth();
        when(authRepositoryPort.findByLoginId("user01")).thenReturn(Optional.of(auth));
        when(loginAttemptPort.isLocked("user01")).thenReturn(false);
        when(loginAttemptPort.getFailCount("user01")).thenReturn(5);
        when(captchaVerificationPort.verify("valid-token")).thenReturn(true);
        when(passwordEncoderPort.matches("wrong", PASSWORD_HASH)).thenReturn(false);
        when(loginAttemptPort.incrementFailCount("user01")).thenReturn(6);

        assertThatThrownBy(() -> authService.signIn(signInRequestWithCaptcha("user01", "wrong", "valid-token")))
                .isInstanceOf(AccountLockedException.class)
                .satisfies(thrown -> {
                    AccountLockedException ex = (AccountLockedException) thrown;
                    assertThat(ex.getMessage()).isEqualTo("로그인 시도가 많아 3분간 로그인이 제한됩니다.");
                    assertThat(ex.getRetryAfterSeconds()).isEqualTo(180L);
                });
    }

    @Test
    void signIn_throwsWhenAccountIsLocked() {
        when(loginAttemptPort.isLocked("user01")).thenReturn(true);
        when(loginAttemptPort.getLockRemainingSeconds("user01")).thenReturn(85L);

        assertThatThrownBy(() -> authService.signIn(signInRequest("user01", "Password1!")))
                .isInstanceOf(AccountLockedException.class)
                .satisfies(thrown -> {
                    AccountLockedException ex = (AccountLockedException) thrown;
                    assertThat(ex.getMessage())
                            .isEqualTo("로그인이 일시적으로 제한되었습니다. 잠시 후 다시 시도해 주세요.");
                    assertThat(ex.getRetryAfterSeconds()).isEqualTo(85L);
                });

        verify(passwordEncoderPort, never()).matches(any(), any());
        verify(captchaVerificationPort, never()).verify(any());
    }

    @Test
    void signIn_rejectsWhenLockedWithoutPasswordCheck() {
        when(loginAttemptPort.isLocked("user01")).thenReturn(true);
        when(loginAttemptPort.getLockRemainingSeconds("user01")).thenReturn(85L);

        assertThatThrownBy(() -> authService.signIn(signInRequest("user01", "wrong")))
                .isInstanceOf(AccountLockedException.class)
                .satisfies(thrown -> {
                    AccountLockedException ex = (AccountLockedException) thrown;
                    assertThat(ex.getRetryAfterSeconds()).isEqualTo(85L);
                });

        verify(loginAttemptPort, never()).incrementFailCount(any());
        verify(authRepositoryPort, never()).findByLoginId(any());
        verify(captchaVerificationPort, never()).verify(any());
    }

    @Test
    void signIn_unknownLoginIdSixthFailureLocksWithSamePolicy() {
        when(loginAttemptPort.isLocked("unknown")).thenReturn(false);
        when(loginAttemptPort.getFailCount("unknown")).thenReturn(5);
        when(authRepositoryPort.findByLoginId("unknown")).thenReturn(Optional.empty());
        when(captchaVerificationPort.verify("valid-token")).thenReturn(true);
        when(loginAttemptPort.incrementFailCount("unknown")).thenReturn(6);

        assertThatThrownBy(() -> authService.signIn(signInRequestWithCaptcha("unknown", "wrong", "valid-token")))
                .isInstanceOf(AccountLockedException.class)
                .satisfies(thrown -> {
                    AccountLockedException ex = (AccountLockedException) thrown;
                    assertThat(ex.getMessage()).isEqualTo("로그인 시도가 많아 1분간 로그인이 제한됩니다.");
                    assertThat(ex.getRetryAfterSeconds()).isEqualTo(60L);
                });

        verify(loginAttemptPort).lock("unknown");
        verify(passwordEncoderPort, never()).matches(any(), any());
    }

    @Test
    void signIn_recordsLoginFailureForUnknownLoginId() {
        when(loginAttemptPort.isLocked("unknown")).thenReturn(false);
        when(loginAttemptPort.getFailCount("unknown")).thenReturn(0);
        when(authRepositoryPort.findByLoginId("unknown")).thenReturn(Optional.empty());
        when(loginAttemptPort.incrementFailCount("unknown")).thenReturn(1);

        assertThatThrownBy(() -> authService.signIn(signInRequest("unknown", "Password1!")))
                .isInstanceOf(UnauthorizedException.class);

        verify(loginAttemptPort).incrementFailCount("unknown");
        verify(passwordEncoderPort, never()).matches(any(), any());
    }

    @Test
    void signIn_unknownLoginIdFifthFailureReturnsCaptchaRequired() {
        when(loginAttemptPort.isLocked("unknown")).thenReturn(false);
        when(loginAttemptPort.getFailCount("unknown")).thenReturn(4);
        when(authRepositoryPort.findByLoginId("unknown")).thenReturn(Optional.empty());
        when(loginAttemptPort.incrementFailCount("unknown")).thenReturn(5);

        assertThatThrownBy(() -> authService.signIn(signInRequest("unknown", "Password1!")))
                .isInstanceOf(CaptchaRequiredException.class);

        verify(passwordEncoderPort, never()).matches(any(), any());
    }

    @Test
    void signIn_resetsLoginFailureOnSuccess() {
        AuthDomain auth = activeAuth();
        when(authRepositoryPort.findByLoginId("user01")).thenReturn(Optional.of(auth));
        when(loginAttemptPort.isLocked("user01")).thenReturn(false);
        when(loginAttemptPort.getFailCount("user01")).thenReturn(3);
        when(passwordEncoderPort.matches("Password1!", PASSWORD_HASH)).thenReturn(true);
        when(jwtProperties.getAccessTokenMinutes()).thenReturn(15L);
        when(tokenProviderPort.createAccessToken(any())).thenReturn("access");
        when(tokenProviderPort.createRefreshToken(any())).thenReturn("refresh");
        when(tokenProviderPort.parseAccessToken("access")).thenReturn(
                ParsedTokenDto.builder()
                        .tokenId("access-id")
                        .authUuid("uuid-001")
                        .tokenType("access")
                        .expiresAt(FIXED_NOW.plusSeconds(900))
                        .build()
        );
        when(tokenProviderPort.parseRefreshToken("refresh")).thenReturn(
                ParsedTokenDto.builder()
                        .tokenId("refresh-id")
                        .authUuid("uuid-001")
                        .tokenType("refresh")
                        .expiresAt(FIXED_NOW.plusSeconds(1_209_600))
                        .build()
        );

        authService.signIn(signInRequest("user01", "Password1!"));

        verify(loginAttemptPort).reset("user01");
    }

    @Test
    void signIn_resetsAfterCaptchaPassAndCorrectPassword() {
        AuthDomain auth = activeAuth();
        when(authRepositoryPort.findByLoginId("user01")).thenReturn(Optional.of(auth));
        when(loginAttemptPort.isLocked("user01")).thenReturn(false);
        when(loginAttemptPort.getFailCount("user01")).thenReturn(5);
        when(captchaVerificationPort.verify("valid-token")).thenReturn(true);
        when(passwordEncoderPort.matches("Password1!", PASSWORD_HASH)).thenReturn(true);
        when(jwtProperties.getAccessTokenMinutes()).thenReturn(15L);
        when(tokenProviderPort.createAccessToken(any())).thenReturn("access");
        when(tokenProviderPort.createRefreshToken(any())).thenReturn("refresh");
        when(tokenProviderPort.parseAccessToken("access")).thenReturn(
                ParsedTokenDto.builder()
                        .tokenId("access-id")
                        .authUuid("uuid-001")
                        .tokenType("access")
                        .expiresAt(FIXED_NOW.plusSeconds(900))
                        .build()
        );
        when(tokenProviderPort.parseRefreshToken("refresh")).thenReturn(
                ParsedTokenDto.builder()
                        .tokenId("refresh-id")
                        .authUuid("uuid-001")
                        .tokenType("refresh")
                        .expiresAt(FIXED_NOW.plusSeconds(1_209_600))
                        .build()
        );

        authService.signIn(signInRequestWithCaptcha("user01", "Password1!", "valid-token"));

        verify(loginAttemptPort).reset("user01");
        verify(loginAttemptPort, never()).incrementFailCount(any());
    }

    private AuthDomain activeAuth() {
        return AuthDomain.reconstitute(
                "uuid-001",
                "user01",
                "홍길동",
                LocalDate.of(1990, 1, 1),
                "01012345678",
                Gender.MALE,
                "user@example.com",
                PASSWORD_HASH,
                Instant.parse("2024-03-01T00:00:00Z"),
                MemberStatus.ACTIVE,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-06-01T00:00:00Z")
        );
    }

    private AuthSignInRequestDto signInRequest(String loginId, String password) {
        return AuthSignInRequestDto.builder()
                .loginId(loginId)
                .password(password)
                .build();
    }

    private AuthSignInRequestDto signInRequestWithIp(String loginId, String password, String clientIp) {
        return AuthSignInRequestDto.builder()
                .loginId(loginId)
                .password(password)
                .clientIp(clientIp)
                .build();
    }

    private AuthSignInRequestDto signInRequestWithCaptcha(String loginId, String password, String captchaToken) {
        return AuthSignInRequestDto.builder()
                .loginId(loginId)
                .password(password)
                .captchaToken(captchaToken)
                .build();
    }
}
