package com.sparta.auth_service.application.service;

import com.sparta.auth_service.application.exception.InvalidTokenException;
import com.sparta.auth_service.application.port.in.dto.AuthLogoutRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthRefreshRequestDto;
import com.sparta.auth_service.application.port.out.AccessTokenBlacklistPort;
import com.sparta.auth_service.application.port.out.AuthRepositoryPort;
import com.sparta.auth_service.application.port.out.FetchIdentityVerificationPort;
import com.sparta.auth_service.application.port.out.IdentityVerificationRepositoryPort;
import com.sparta.auth_service.application.port.out.PasswordEncoderPort;
import com.sparta.auth_service.application.port.out.RefreshTokenPort;
import com.sparta.auth_service.application.port.out.TokenProviderPort;
import com.sparta.auth_service.application.port.out.dto.ParsedTokenDto;
import com.sparta.auth_service.domain.enums.Gender;
import com.sparta.auth_service.domain.model.AuthDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshLogoutTest {

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

    @Test
    void refresh_rotatesRefreshTokenInRedis() {
        when(tokenProviderPort.parseRefreshToken("old-refresh")).thenReturn(
                ParsedTokenDto.builder()
                        .tokenId("old-jti")
                        .authUuid("uuid-001")
                        .tokenType("refresh")
                        .build()
        );
        when(refreshTokenPort.matches("uuid-001", "old-jti")).thenReturn(true);
        when(authRepositoryPort.findByAuthUuid("uuid-001")).thenReturn(Optional.of(auth()));
        when(tokenProviderPort.createAccessToken("uuid-001")).thenReturn("new-access");
        when(tokenProviderPort.createRefreshToken("uuid-001")).thenReturn("new-refresh");
        when(tokenProviderPort.parseRefreshToken("new-refresh")).thenReturn(
                ParsedTokenDto.builder()
                        .tokenId("new-jti")
                        .authUuid("uuid-001")
                        .tokenType("refresh")
                        .build()
        );

        var result = authService.refresh(AuthRefreshRequestDto.builder().refreshToken("old-refresh").build());

        verify(refreshTokenPort).delete("uuid-001");
        verify(refreshTokenPort).save(eq("uuid-001"), eq("new-jti"), any(Long.class));
        assertThat(result.getAccessToken()).isEqualTo("new-access");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh");
        assertThat(result.getRole()).isEqualTo("USER");
    }

    @Test
    void refresh_throwsWhenRefreshTokenMissing() {
        assertThatThrownBy(() -> authService.refresh(AuthRefreshRequestDto.builder().refreshToken("").build()))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void logout_deletesRefreshAndBlacklistsAccess() {
        Instant expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES);
        when(tokenProviderPort.parseRefreshToken("refresh")).thenReturn(
                ParsedTokenDto.builder()
                        .tokenId("refresh-jti")
                        .authUuid("uuid-001")
                        .tokenType("refresh")
                        .expiresAt(expiresAt)
                        .build()
        );
        when(tokenProviderPort.parseAccessToken("access")).thenReturn(
                ParsedTokenDto.builder()
                        .tokenId("access-jti")
                        .authUuid("uuid-001")
                        .tokenType("access")
                        .expiresAt(expiresAt)
                        .build()
        );

        authService.logout(AuthLogoutRequestDto.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build());

        verify(refreshTokenPort).delete("uuid-001");
        verify(accessTokenBlacklistPort).blacklist(eq("access-jti"), any(Long.class));
    }

    @Test
    void logout_skipsBlacklistWhenAccessTokenMissing() {
        when(tokenProviderPort.parseRefreshToken("refresh")).thenReturn(
                ParsedTokenDto.builder()
                        .tokenId("refresh-jti")
                        .authUuid("uuid-001")
                        .tokenType("refresh")
                        .build()
        );

        authService.logout(AuthLogoutRequestDto.builder()
                .refreshToken("refresh")
                .build());

        verify(refreshTokenPort).delete("uuid-001");
        verify(accessTokenBlacklistPort, never()).blacklist(any(), any(Long.class));
    }

    private AuthDomain auth() {
        return AuthDomain.reconstitute(
                "uuid-001",
                "user01",
                "홍길동",
                LocalDate.of(1990, 1, 1),
                "01012345678",
                Gender.MALE,
                "user@example.com",
                "ci-value-001",
                "$2a$10$hash",
                Instant.parse("2024-03-01T00:00:00Z"),
                0,
                null,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-06-01T00:00:00Z")
        );
    }
}
