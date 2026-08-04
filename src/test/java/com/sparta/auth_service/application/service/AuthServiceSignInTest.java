package com.sparta.auth_service.application.service;

import com.sparta.auth_service.application.exception.AccountLockedException;
import com.sparta.auth_service.application.exception.UnauthorizedException;
import com.sparta.auth_service.application.port.in.dto.AuthSignInRequestDto;
import com.sparta.auth_service.application.port.out.AccessTokenBlacklistPort;
import com.sparta.auth_service.application.port.out.AuthRepositoryPort;
import com.sparta.auth_service.application.port.out.FetchIdentityVerificationPort;
import com.sparta.auth_service.application.port.out.IdentityVerificationRepositoryPort;
import com.sparta.auth_service.application.port.out.PasswordEncoderPort;
import com.sparta.auth_service.application.port.out.RefreshTokenPort;
import com.sparta.auth_service.application.port.out.TokenProviderPort;
import com.sparta.auth_service.application.port.out.dto.ParsedTokenDto;
import com.sparta.auth_service.domain.model.AuthDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private AccessTokenBlacklistPort accessTokenBlacklistPort;

    @Mock
    private IdentityVerificationRepositoryPort identityVerificationRepositoryPort;

    @Mock
    private FetchIdentityVerificationPort fetchIdentityVerificationPort;

    @InjectMocks
    private AuthService authService;

    private static final String PASSWORD_HASH = "$2a$10$encodedhashvalueplaceholder";

    @Test
    void signIn_throwsWhenAccountIsLocked() {
        Instant lockedUntil = Instant.parse("2030-01-01T00:00:00Z");
        AuthDomain lockedAuth = auth(4, lockedUntil);
        when(authRepositoryPort.findByLoginId("user01")).thenReturn(Optional.of(lockedAuth));

        assertThatThrownBy(() -> authService.signIn(signInRequest("user01", "Password1!")))
                .isInstanceOf(AccountLockedException.class);

        verify(authRepositoryPort, never()).save(any());
    }

    @Test
    void signIn_recordsLoginFailureWhenPasswordIsWrong() {
        AuthDomain auth = auth(0, null);
        when(authRepositoryPort.findByLoginId("user01")).thenReturn(Optional.of(auth));
        when(passwordEncoderPort.matches("wrong", PASSWORD_HASH)).thenReturn(false);
        when(authRepositoryPort.save(any(AuthDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> authService.signIn(signInRequest("user01", "wrong")))
                .isInstanceOf(UnauthorizedException.class);

        ArgumentCaptor<AuthDomain> captor = ArgumentCaptor.forClass(AuthDomain.class);
        verify(authRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().getLoginFailCount()).isEqualTo(1);
    }

    @Test
    void signIn_resetsLoginFailureOnSuccess() {
        AuthDomain auth = auth(3, null);
        when(authRepositoryPort.findByLoginId("user01")).thenReturn(Optional.of(auth));
        when(passwordEncoderPort.matches("Password1!", PASSWORD_HASH)).thenReturn(true);
        when(tokenProviderPort.createAccessToken(any())).thenReturn("access");
        when(tokenProviderPort.createRefreshToken(any())).thenReturn("refresh");
        when(tokenProviderPort.parseRefreshToken("refresh")).thenReturn(
                ParsedTokenDto.builder()
                        .tokenId("refresh-id")
                        .authUuid("uuid-001")
                        .tokenType("refresh")
                        .build()
        );
        when(authRepositoryPort.save(any(AuthDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.signIn(signInRequest("user01", "Password1!"));

        ArgumentCaptor<AuthDomain> captor = ArgumentCaptor.forClass(AuthDomain.class);
        verify(authRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().getLoginFailCount()).isZero();
        assertThat(captor.getValue().getLockedUntil()).isNull();
    }

    private AuthDomain auth(int loginFailCount, Instant lockedUntil) {
        return AuthDomain.reconstitute(
                "uuid-001",
                "user01",
                "홍길동",
                LocalDate.of(1990, 1, 1),
                "01012345678",
                "user@example.com",
                "ci-value-001",
                PASSWORD_HASH,
                Instant.parse("2024-03-01T00:00:00Z"),
                loginFailCount,
                lockedUntil,
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
}
