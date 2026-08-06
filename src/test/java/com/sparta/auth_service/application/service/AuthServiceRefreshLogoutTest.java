package com.sparta.auth_service.application.service;



import com.sparta.auth_service.adaptor.in.web.config.LoginAttemptProperties;

import com.sparta.auth_service.adaptor.out.security.JwtProperties;

import com.sparta.auth_service.application.exception.InvalidTokenException;

import com.sparta.auth_service.application.exception.MemberNotActiveException;
import com.sparta.auth_service.application.exception.SecurityStoreUnavailableException;

import com.sparta.auth_service.application.port.in.dto.AuthLogoutRequestDto;

import com.sparta.auth_service.application.port.in.dto.AuthRefreshRequestDto;

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

import com.sparta.auth_service.application.port.out.dto.RefreshTokenRotationResult;

import com.sparta.auth_service.domain.enums.Gender;

import com.sparta.auth_service.domain.enums.MemberStatus;

import com.sparta.auth_service.domain.model.AuthDomain;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;

import java.time.LocalDate;

import java.time.temporal.ChronoUnit;

import java.util.Optional;



import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;



@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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



    @BeforeEach

    void setUpPolicy() {

        when(clock.instant()).thenReturn(FIXED_NOW);

        when(loginAttemptProperties.getCaptchaThreshold()).thenReturn(5);

        when(loginAttemptProperties.getLockThreshold()).thenReturn(6);

        when(loginRateLimitPort.checkAndRecord(any())).thenReturn(LoginRateLimitResultDto.allowed());

    }



    @Test

    void refresh_savesActiveAccessOnlyAfterRotationSucceeds() {

        Instant accessExpires = FIXED_NOW.plus(30, ChronoUnit.MINUTES);

        Instant refreshExpires = FIXED_NOW.plus(14, ChronoUnit.DAYS);



        when(tokenProviderPort.parseRefreshToken("old-refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("old-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .expiresAt(refreshExpires)

                        .build()

        );

        when(authRepositoryPort.findByAuthUuid("uuid-001")).thenReturn(Optional.of(auth()));

        when(tokenProviderPort.createAccessToken("uuid-001")).thenReturn("new-access");

        when(tokenProviderPort.createRefreshToken("uuid-001")).thenReturn("new-refresh");

        when(tokenProviderPort.parseAccessToken("new-access")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-access-jti")

                        .authUuid("uuid-001")

                        .tokenType("access")

                        .expiresAt(accessExpires)

                        .build()

        );

        when(tokenProviderPort.parseRefreshToken("new-refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .expiresAt(refreshExpires)

                        .build()

        );

        when(refreshTokenPort.rotate(eq("uuid-001"), eq("old-jti"), eq("new-jti"), any(Long.class)))
                .thenReturn(RefreshTokenRotationResult.SUCCESS);



        authService.refresh(new AuthRefreshRequestDto("old-refresh", null));



        var order = inOrder(refreshTokenPort, activeAccessTokenPort);

        order.verify(refreshTokenPort).rotate(eq("uuid-001"), eq("old-jti"), eq("new-jti"), any(Long.class));

        order.verify(activeAccessTokenPort).save(eq("uuid-001"), eq("new-access-jti"), any(Long.class));

    }



    @Test

    void refresh_rotatesRefreshTokenAtomically() {

        Instant accessExpires = FIXED_NOW.plus(30, ChronoUnit.MINUTES);

        Instant refreshExpires = FIXED_NOW.plus(14, ChronoUnit.DAYS);



        when(tokenProviderPort.parseRefreshToken("old-refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("old-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .expiresAt(refreshExpires)

                        .build()

        );

        when(authRepositoryPort.findByAuthUuid("uuid-001")).thenReturn(Optional.of(auth()));

        when(tokenProviderPort.createAccessToken("uuid-001")).thenReturn("new-access");

        when(tokenProviderPort.createRefreshToken("uuid-001")).thenReturn("new-refresh");

        when(tokenProviderPort.parseAccessToken("new-access")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-access-jti")

                        .authUuid("uuid-001")

                        .tokenType("access")

                        .expiresAt(accessExpires)

                        .build()

        );

        when(tokenProviderPort.parseRefreshToken("new-refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .expiresAt(refreshExpires)

                        .build()

        );

        when(refreshTokenPort.rotate(eq("uuid-001"), eq("old-jti"), eq("new-jti"), any(Long.class)))
                .thenReturn(RefreshTokenRotationResult.SUCCESS);



        var result = authService.refresh(new AuthRefreshRequestDto("old-refresh", null));



        verify(refreshTokenPort).rotate(eq("uuid-001"), eq("old-jti"), eq("new-jti"), any(Long.class));

        verify(refreshTokenPort, never()).delete("uuid-001");

        verify(activeAccessTokenPort).save(eq("uuid-001"), eq("new-access-jti"), any(Long.class));

        verify(accessTokenBlacklistPort, never()).blacklist(any(), any(Long.class));

        assertThat(result.getAccessToken()).isEqualTo("new-access");

        assertThat(result.getRefreshToken()).isEqualTo("new-refresh");

        assertThat(result.getRole()).isEqualTo("USER");

    }



    @Test

    void refresh_throwsWhenRotationFails() {

        when(tokenProviderPort.parseRefreshToken("old-refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("old-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .expiresAt(FIXED_NOW.plus(1, ChronoUnit.DAYS))

                        .build()

        );

        when(authRepositoryPort.findByAuthUuid("uuid-001")).thenReturn(Optional.of(auth()));

        when(tokenProviderPort.createAccessToken("uuid-001")).thenReturn("new-access");

        when(tokenProviderPort.createRefreshToken("uuid-001")).thenReturn("new-refresh");

        when(tokenProviderPort.parseAccessToken("new-access")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-access-jti")

                        .authUuid("uuid-001")

                        .tokenType("access")

                        .expiresAt(FIXED_NOW.plus(15, ChronoUnit.MINUTES))

                        .build()

        );

        when(tokenProviderPort.parseRefreshToken("new-refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .expiresAt(FIXED_NOW.plus(14, ChronoUnit.DAYS))

                        .build()

        );

        when(refreshTokenPort.rotate(any(), any(), any(), any(Long.class)))
                .thenReturn(RefreshTokenRotationResult.JTI_MISMATCH);



        assertThatThrownBy(() -> authService.refresh(

                new AuthRefreshRequestDto("old-refresh", null)

        )).isInstanceOf(InvalidTokenException.class);



        verify(activeAccessTokenPort, never()).save(any(), any(), any(Long.class));

    }



    @Test

    void refresh_throwsInvalidTokenWhenJtiMismatchAndAccessBlacklisted() {

        stubRefreshTokenCreationMocks();

        when(refreshTokenPort.rotate(any(), any(), any(), any(Long.class)))
                .thenReturn(RefreshTokenRotationResult.JTI_MISMATCH);

        when(tokenProviderPort.parseAccessToken("old-access")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("old-access-jti")

                        .authUuid("uuid-001")

                        .tokenType("access")

                        .expiresAt(FIXED_NOW.plus(15, ChronoUnit.MINUTES))

                        .build()

        );

        when(accessTokenBlacklistPort.isBlacklisted("old-access-jti")).thenReturn(true);



        assertThatThrownBy(() -> authService.refresh(

                new AuthRefreshRequestDto("old-refresh", "old-access")

        )).isInstanceOf(InvalidTokenException.class);



        verify(activeAccessTokenPort, never()).save(any(), any(), any(Long.class));

    }



    @Test

    void refresh_throwsInvalidTokenWhenRedisRefreshKeyMissing() {

        stubRefreshTokenCreationMocks();

        when(refreshTokenPort.rotate(any(), any(), any(), any(Long.class)))
                .thenReturn(RefreshTokenRotationResult.KEY_NOT_FOUND);



        assertThatThrownBy(() -> authService.refresh(

                new AuthRefreshRequestDto("old-refresh", null)

        )).isInstanceOf(InvalidTokenException.class);



        verify(activeAccessTokenPort, never()).save(any(), any(), any(Long.class));

    }



    @Test

    void refresh_blocksInactiveAccountAndRevokesSession() {

        AuthDomain inactive = inactiveAuth();

        when(tokenProviderPort.parseRefreshToken("refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("refresh-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .build()

        );

        when(authRepositoryPort.findByAuthUuid("uuid-001")).thenReturn(Optional.of(inactive));

        when(jwtProperties.getAccessTokenMinutes()).thenReturn(15L);

        when(activeAccessTokenPort.find("uuid-001")).thenReturn(Optional.of("access-jti"));



        assertThatThrownBy(() -> authService.refresh(

                new AuthRefreshRequestDto("refresh", null)

        )).isInstanceOf(MemberNotActiveException.class);



        verify(refreshTokenPort).delete("uuid-001");

        verify(activeAccessTokenPort).delete("uuid-001");

        verify(accessTokenBlacklistPort).blacklist(eq("access-jti"), eq(900L));

        verify(tokenProviderPort, never()).createAccessToken(any());

        verify(tokenProviderPort, never()).createRefreshToken(any());

        verify(refreshTokenPort, never()).rotate(any(), any(), any(), any(Long.class));

        var inOrder = inOrder(activeAccessTokenPort, accessTokenBlacklistPort, refreshTokenPort);

        inOrder.verify(activeAccessTokenPort).find("uuid-001");

        inOrder.verify(accessTokenBlacklistPort).blacklist(eq("access-jti"), eq(900L));

        inOrder.verify(activeAccessTokenPort).delete("uuid-001");

        inOrder.verify(refreshTokenPort).delete("uuid-001");

    }



    @ParameterizedTest

    @EnumSource(value = MemberStatus.class, names = {"SUSPENDED", "WITHDRAWN", "DORMANT"})

    void refresh_blocksEachInactiveMemberStatus(MemberStatus memberStatus) {

        AuthDomain inactive = authWithStatus(memberStatus);

        when(tokenProviderPort.parseRefreshToken("refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("refresh-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .build()

        );

        when(authRepositoryPort.findByAuthUuid("uuid-001")).thenReturn(Optional.of(inactive));



        assertThatThrownBy(() -> authService.refresh(

                new AuthRefreshRequestDto("refresh", null)

        )).isInstanceOf(MemberNotActiveException.class);



        verify(tokenProviderPort, never()).createAccessToken(any());

        verify(tokenProviderPort, never()).createRefreshToken(any());

        verify(refreshTokenPort, never()).rotate(any(), any(), any(), any(Long.class));

        verify(refreshTokenPort).delete("uuid-001");

    }



    @Test

    void refresh_blocksInactiveAccountWhenNoActiveAccessJti() {

        AuthDomain inactive = inactiveAuth();

        when(tokenProviderPort.parseRefreshToken("refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("refresh-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .build()

        );

        when(authRepositoryPort.findByAuthUuid("uuid-001")).thenReturn(Optional.of(inactive));

        when(activeAccessTokenPort.find("uuid-001")).thenReturn(Optional.empty());



        assertThatThrownBy(() -> authService.refresh(

                new AuthRefreshRequestDto("refresh", null)

        )).isInstanceOf(MemberNotActiveException.class);



        verify(accessTokenBlacklistPort, never()).blacklist(any(), any(Long.class));

        verify(activeAccessTokenPort).delete("uuid-001");

        verify(refreshTokenPort).delete("uuid-001");

        verify(tokenProviderPort, never()).createAccessToken(any());

        verify(refreshTokenPort, never()).rotate(any(), any(), any(), any(Long.class));

    }



    @Test

    void refresh_skipsBlacklistWhenConfiguredAccessTtlIsZero() {

        AuthDomain inactive = inactiveAuth();

        when(tokenProviderPort.parseRefreshToken("refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("refresh-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .build()

        );

        when(authRepositoryPort.findByAuthUuid("uuid-001")).thenReturn(Optional.of(inactive));

        when(jwtProperties.getAccessTokenMinutes()).thenReturn(0L);

        when(activeAccessTokenPort.find("uuid-001")).thenReturn(Optional.of("access-jti"));



        assertThatThrownBy(() -> authService.refresh(

                new AuthRefreshRequestDto("refresh", null)

        )).isInstanceOf(MemberNotActiveException.class);



        verify(accessTokenBlacklistPort).blacklist("access-jti", 0L);

        verify(activeAccessTokenPort).delete("uuid-001");

        verify(refreshTokenPort).delete("uuid-001");

    }



    @Test

    void refresh_throwsWhenRefreshTokenMissing() {

        assertThatThrownBy(() -> authService.refresh(new AuthRefreshRequestDto("", null)))

                .isInstanceOf(InvalidTokenException.class);

    }



    @Test

    void logout_deletesRefreshAndBlacklistsAccess() {

        Instant expiresAt = FIXED_NOW.plus(10, ChronoUnit.MINUTES);

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



        verify(refreshTokenPort).deleteIfMatches("uuid-001", "refresh-jti");

        verify(accessTokenBlacklistPort).blacklist(eq("access-jti"), any(Long.class));

        verify(activeAccessTokenPort).deleteIfMatches("uuid-001", "access-jti");

    }



    @Test

    void logout_throwsSecurityStoreUnavailableWhenBlacklistFails() {

        Instant expiresAt = FIXED_NOW.plus(10, ChronoUnit.MINUTES);

        when(tokenProviderPort.parseAccessToken("access")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("access-jti")

                        .authUuid("uuid-001")

                        .tokenType("access")

                        .expiresAt(expiresAt)

                        .build()

        );

        org.mockito.Mockito.doThrow(new SecurityStoreUnavailableException(new RuntimeException("redis down")))

                .when(accessTokenBlacklistPort).blacklist(eq("access-jti"), any(Long.class));



        assertThatThrownBy(() -> authService.logout(AuthLogoutRequestDto.builder()

                .accessToken("access")

                .build()))

                .isInstanceOf(SecurityStoreUnavailableException.class);

    }



    @Test

    void refresh_inactiveRevokeFailureDoesNotMaskWithMemberNotActive() {

        AuthDomain inactive = inactiveAuth();

        when(tokenProviderPort.parseRefreshToken("refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("refresh-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .build()

        );

        when(authRepositoryPort.findByAuthUuid("uuid-001")).thenReturn(Optional.of(inactive));

        when(activeAccessTokenPort.find("uuid-001")).thenReturn(Optional.of("access-jti"));

        org.mockito.Mockito.doThrow(new SecurityStoreUnavailableException(new RuntimeException("redis down")))

                .when(accessTokenBlacklistPort).blacklist(eq("access-jti"), any(Long.class));



        assertThatThrownBy(() -> authService.refresh(

                new AuthRefreshRequestDto("refresh", null)

        )).isInstanceOf(SecurityStoreUnavailableException.class)

                .isNotInstanceOf(MemberNotActiveException.class);

    }



    @Test

    void signIn_usesJwtConfigForRedisTtl() {

        AuthDomain auth = auth();

        when(authRepositoryPort.findByLoginId("user01")).thenReturn(Optional.of(auth));

        when(loginAttemptPort.isLocked("user01")).thenReturn(false);

        when(loginAttemptPort.getFailCount("user01")).thenReturn(0);

        when(passwordEncoderPort.matches("Password1!", auth.getPasswordHash())).thenReturn(true);

        when(jwtProperties.getAccessTokenMinutes()).thenReturn(30L);

        when(tokenProviderPort.createAccessToken("uuid-001")).thenReturn("new-access");

        when(tokenProviderPort.createRefreshToken("uuid-001")).thenReturn("new-refresh");

        Instant accessExpires = FIXED_NOW.plus(30, ChronoUnit.MINUTES);

        Instant refreshExpires = FIXED_NOW.plus(14, ChronoUnit.DAYS);

        when(tokenProviderPort.parseAccessToken("new-access")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-access-jti")

                        .authUuid("uuid-001")

                        .tokenType("access")

                        .expiresAt(accessExpires)

                        .build()

        );

        when(tokenProviderPort.parseRefreshToken("new-refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-refresh-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .expiresAt(refreshExpires)

                        .build()

        );



        authService.signIn(AuthSignInRequestDto.builder()

                .loginId("user01")

                .password("Password1!")

                .build());



        verify(activeAccessTokenPort).save(eq("uuid-001"), eq("new-access-jti"), any(Long.class));

        verify(refreshTokenPort).save(eq("uuid-001"), eq("new-refresh-jti"), any(Long.class));

    }



    @Test

    void refresh_usesCeilTtlFromJwtExpiresAt() {

        Instant accessExpires = FIXED_NOW.plusSeconds(900).plusNanos(1);

        Instant refreshExpires = FIXED_NOW.plus(14, ChronoUnit.DAYS);



        when(tokenProviderPort.parseRefreshToken("old-refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("old-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .expiresAt(refreshExpires)

                        .build()

        );

        when(authRepositoryPort.findByAuthUuid("uuid-001")).thenReturn(Optional.of(auth()));

        when(tokenProviderPort.createAccessToken("uuid-001")).thenReturn("new-access");

        when(tokenProviderPort.createRefreshToken("uuid-001")).thenReturn("new-refresh");

        when(tokenProviderPort.parseAccessToken("new-access")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-access-jti")

                        .authUuid("uuid-001")

                        .tokenType("access")

                        .expiresAt(accessExpires)

                        .build()

        );

        when(tokenProviderPort.parseRefreshToken("new-refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .expiresAt(refreshExpires)

                        .build()

        );

        when(refreshTokenPort.rotate(any(), any(), any(), any(Long.class)))
                .thenReturn(RefreshTokenRotationResult.SUCCESS);



        authService.refresh(new AuthRefreshRequestDto("old-refresh", null));



        ArgumentCaptor<Long> refreshTtlCaptor = ArgumentCaptor.forClass(Long.class);

        verify(refreshTokenPort).rotate(eq("uuid-001"), eq("old-jti"), eq("new-jti"), refreshTtlCaptor.capture());

        assertThat(refreshTtlCaptor.getValue()).isEqualTo(1_209_600L);



        ArgumentCaptor<Long> accessTtlCaptor = ArgumentCaptor.forClass(Long.class);

        verify(activeAccessTokenPort).save(eq("uuid-001"), eq("new-access-jti"), accessTtlCaptor.capture());

        assertThat(accessTtlCaptor.getValue()).isEqualTo(901L);

    }



    @Test

    void logout_passesZeroTtlWhenAccessTokenAlreadyExpired() {

        when(tokenProviderPort.parseAccessToken("access")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("access-jti")

                        .authUuid("uuid-001")

                        .tokenType("access")

                        .expiresAt(FIXED_NOW.minusSeconds(1))

                        .build()

        );



        authService.logout(AuthLogoutRequestDto.builder().accessToken("access").build());



        verify(accessTokenBlacklistPort).blacklist("access-jti", 0L);

        verify(activeAccessTokenPort).deleteIfMatches("uuid-001", "access-jti");

    }



    @Test

    void logout_doesNotDeleteLatestSessionKeysWhenTokensAreStale() {

        Instant expiresAt = FIXED_NOW.plus(10, ChronoUnit.MINUTES);

        when(tokenProviderPort.parseRefreshToken("stale-refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("stale-refresh-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .expiresAt(expiresAt)

                        .build()

        );

        when(tokenProviderPort.parseAccessToken("stale-access")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("stale-access-jti")

                        .authUuid("uuid-001")

                        .tokenType("access")

                        .expiresAt(expiresAt)

                        .build()

        );

        when(refreshTokenPort.deleteIfMatches("uuid-001", "stale-refresh-jti")).thenReturn(false);

        when(activeAccessTokenPort.deleteIfMatches("uuid-001", "stale-access-jti")).thenReturn(false);



        authService.logout(AuthLogoutRequestDto.builder()

                .accessToken("stale-access")

                .refreshToken("stale-refresh")

                .build());



        verify(refreshTokenPort).deleteIfMatches("uuid-001", "stale-refresh-jti");

        verify(accessTokenBlacklistPort).blacklist(eq("stale-access-jti"), any(Long.class));

        verify(activeAccessTokenPort).deleteIfMatches("uuid-001", "stale-access-jti");

        verify(refreshTokenPort, never()).delete(any());

        verify(activeAccessTokenPort, never()).delete(any());

    }



    @Test

    void logout_deletesSessionKeysWhenTokensMatchCurrentRedisState() {

        Instant expiresAt = FIXED_NOW.plus(10, ChronoUnit.MINUTES);

        when(tokenProviderPort.parseRefreshToken("current-refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("current-refresh-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .expiresAt(expiresAt)

                        .build()

        );

        when(tokenProviderPort.parseAccessToken("current-access")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("current-access-jti")

                        .authUuid("uuid-001")

                        .tokenType("access")

                        .expiresAt(expiresAt)

                        .build()

        );

        when(refreshTokenPort.deleteIfMatches("uuid-001", "current-refresh-jti")).thenReturn(true);

        when(activeAccessTokenPort.deleteIfMatches("uuid-001", "current-access-jti")).thenReturn(true);



        authService.logout(AuthLogoutRequestDto.builder()

                .accessToken("current-access")

                .refreshToken("current-refresh")

                .build());



        verify(refreshTokenPort).deleteIfMatches("uuid-001", "current-refresh-jti");

        verify(accessTokenBlacklistPort).blacklist(eq("current-access-jti"), any(Long.class));

        verify(activeAccessTokenPort).deleteIfMatches("uuid-001", "current-access-jti");

        verify(refreshTokenPort, never()).delete(any());

        verify(activeAccessTokenPort, never()).delete(any());

    }



    @Test

    void signIn_blacklistsPreviousJtiWithConfiguredAccessLifetime() {

        AuthDomain auth = auth();

        when(authRepositoryPort.findByLoginId("user01")).thenReturn(Optional.of(auth));

        when(loginAttemptPort.isLocked("user01")).thenReturn(false);

        when(loginAttemptPort.getFailCount("user01")).thenReturn(0);

        when(passwordEncoderPort.matches("Password1!", auth.getPasswordHash())).thenReturn(true);

        when(jwtProperties.getAccessTokenMinutes()).thenReturn(30L);

        when(activeAccessTokenPort.find("uuid-001")).thenReturn(Optional.of("previous-jti"));

        when(tokenProviderPort.createAccessToken("uuid-001")).thenReturn("new-access");

        when(tokenProviderPort.createRefreshToken("uuid-001")).thenReturn("new-refresh");

        when(tokenProviderPort.parseAccessToken("new-access")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-access-jti")

                        .authUuid("uuid-001")

                        .tokenType("access")

                        .expiresAt(FIXED_NOW.plus(30, ChronoUnit.MINUTES))

                        .build()

        );

        when(tokenProviderPort.parseRefreshToken("new-refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-refresh-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .expiresAt(FIXED_NOW.plus(14, ChronoUnit.DAYS))

                        .build()

        );



        authService.signIn(AuthSignInRequestDto.builder()

                .loginId("user01")

                .password("Password1!")

                .build());



        // Redis auth:access 값은 jti만 저장 — 이전 토큰 expiresAt 없어 설정 access 전체 수명 TTL 사용

        verify(accessTokenBlacklistPort).blacklist("previous-jti", 1_800L);

    }



    @Test

    void signIn_usesCeilTtlForNewTokens() {

        AuthDomain auth = auth();

        when(authRepositoryPort.findByLoginId("user01")).thenReturn(Optional.of(auth));

        when(loginAttemptPort.isLocked("user01")).thenReturn(false);

        when(loginAttemptPort.getFailCount("user01")).thenReturn(0);

        when(passwordEncoderPort.matches("Password1!", auth.getPasswordHash())).thenReturn(true);

        when(activeAccessTokenPort.find("uuid-001")).thenReturn(Optional.empty());

        when(tokenProviderPort.createAccessToken("uuid-001")).thenReturn("new-access");

        when(tokenProviderPort.createRefreshToken("uuid-001")).thenReturn("new-refresh");

        Instant accessExpires = FIXED_NOW.plusNanos(1_000_000);

        Instant refreshExpires = FIXED_NOW.plusSeconds(1).plusNanos(1);

        when(tokenProviderPort.parseAccessToken("new-access")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-access-jti")

                        .authUuid("uuid-001")

                        .tokenType("access")

                        .expiresAt(accessExpires)

                        .build()

        );

        when(tokenProviderPort.parseRefreshToken("new-refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-refresh-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .expiresAt(refreshExpires)

                        .build()

        );



        authService.signIn(AuthSignInRequestDto.builder()

                .loginId("user01")

                .password("Password1!")

                .build());



        ArgumentCaptor<Long> accessTtlCaptor = ArgumentCaptor.forClass(Long.class);

        verify(activeAccessTokenPort).save(eq("uuid-001"), eq("new-access-jti"), accessTtlCaptor.capture());

        assertThat(accessTtlCaptor.getValue()).isEqualTo(1L);



        ArgumentCaptor<Long> refreshTtlCaptor = ArgumentCaptor.forClass(Long.class);

        verify(refreshTokenPort).save(eq("uuid-001"), eq("new-refresh-jti"), refreshTtlCaptor.capture());

        assertThat(refreshTtlCaptor.getValue()).isEqualTo(2L);

    }



    private void stubRefreshTokenCreationMocks() {

        when(tokenProviderPort.parseRefreshToken("old-refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("old-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .expiresAt(FIXED_NOW.plus(1, ChronoUnit.DAYS))

                        .build()

        );

        when(authRepositoryPort.findByAuthUuid("uuid-001")).thenReturn(Optional.of(auth()));

        when(tokenProviderPort.createAccessToken("uuid-001")).thenReturn("new-access");

        when(tokenProviderPort.createRefreshToken("uuid-001")).thenReturn("new-refresh");

        when(tokenProviderPort.parseAccessToken("new-access")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-access-jti")

                        .authUuid("uuid-001")

                        .tokenType("access")

                        .expiresAt(FIXED_NOW.plus(15, ChronoUnit.MINUTES))

                        .build()

        );

        when(tokenProviderPort.parseRefreshToken("new-refresh")).thenReturn(

                ParsedTokenDto.builder()

                        .tokenId("new-jti")

                        .authUuid("uuid-001")

                        .tokenType("refresh")

                        .expiresAt(FIXED_NOW.plus(14, ChronoUnit.DAYS))

                        .build()

        );

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

                "$2a$10$hash",

                Instant.parse("2024-03-01T00:00:00Z"),

                MemberStatus.ACTIVE,

                Instant.parse("2024-01-01T00:00:00Z"),

                Instant.parse("2024-06-01T00:00:00Z")

        );

    }



    private AuthDomain inactiveAuth() {

        return authWithStatus(MemberStatus.SUSPENDED);

    }



    private AuthDomain authWithStatus(MemberStatus memberStatus) {

        return AuthDomain.reconstitute(

                "uuid-001",

                "user01",

                "홍길동",

                LocalDate.of(1990, 1, 1),

                "01012345678",

                Gender.MALE,

                "user@example.com",

                "$2a$10$hash",

                Instant.parse("2024-03-01T00:00:00Z"),

                memberStatus,

                Instant.parse("2024-01-01T00:00:00Z"),

                Instant.parse("2024-06-01T00:00:00Z")

        );

    }

}

