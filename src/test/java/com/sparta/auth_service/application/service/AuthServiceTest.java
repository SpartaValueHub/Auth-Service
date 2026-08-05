package com.sparta.auth_service.application.service;

import com.sparta.auth_service.adaptor.in.web.config.LoginAttemptProperties;
import com.sparta.auth_service.adaptor.out.security.JwtProperties;
import com.sparta.auth_service.application.exception.IdentityVerificationNotReadyException;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpRequestDto;
import com.sparta.auth_service.application.port.out.AccessTokenBlacklistPort;
import com.sparta.auth_service.application.port.out.ActiveAccessTokenPort;
import com.sparta.auth_service.application.port.out.AuthRepositoryPort;
import com.sparta.auth_service.application.port.out.CaptchaVerificationPort;
import com.sparta.auth_service.application.port.out.FetchIdentityVerificationPort;
import com.sparta.auth_service.application.port.out.IdentityKeyHashPort;
import com.sparta.auth_service.application.port.out.IdentityVerificationRepositoryPort;
import com.sparta.auth_service.application.port.out.LoginAttemptPort;
import com.sparta.auth_service.application.port.out.LoginRateLimitPort;
import com.sparta.auth_service.application.port.out.PasswordEncoderPort;
import com.sparta.auth_service.application.port.out.RefreshTokenPort;
import com.sparta.auth_service.application.port.out.TokenProviderPort;
import com.sparta.auth_service.application.port.out.dto.ExternalIdentityVerificationDto;
import com.sparta.auth_service.domain.enums.Gender;
import com.sparta.auth_service.domain.enums.VerificationMethod;
import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.model.AuthDomain;
import com.sparta.auth_service.domain.model.IdentityVerificationDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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

    @Test
    void signUp_usesVerifiedCustomerFromPortOne() {
        IdentityVerificationDomain verification = verifiedVerification("verify-001");

        ExternalIdentityVerificationDto external = ExternalIdentityVerificationDto.builder()
                .requestToken("verify-001")
                .portOneStatus("VERIFIED")
                .identityKey("ci-value-001")
                .memberName("홍길동")
                .phoneNumber("01012345678")
                .birthdayDate(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .build();

        when(identityVerificationRepositoryPort.findByRequestToken("verify-001"))
                .thenReturn(Optional.of(verification));
        when(fetchIdentityVerificationPort.fetchByRequestToken("verify-001"))
                .thenReturn(Optional.of(external));
        when(identityKeyHashPort.hashForLookup("ci-value-001")).thenReturn("ci-hash-001");
        when(passwordEncoderPort.encode("Password1!")).thenReturn("encoded-hash");
        when(authRepositoryPort.save(any(AuthDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(identityVerificationRepositoryPort.save(any(IdentityVerificationDomain.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.signUp(signUpRequest());

        ArgumentCaptor<AuthDomain> authCaptor = ArgumentCaptor.forClass(AuthDomain.class);
        verify(authRepositoryPort).save(authCaptor.capture());
        assertThat(authCaptor.getValue().getMemberName()).isEqualTo("홍길동");
        assertThat(authCaptor.getValue().getPasswordHash()).isEqualTo("encoded-hash");
        assertThat(authCaptor.getValue().getMemberStatus()).isNotNull();

        ArgumentCaptor<IdentityVerificationDomain> verificationCaptor =
                ArgumentCaptor.forClass(IdentityVerificationDomain.class);
        verify(identityVerificationRepositoryPort).save(verificationCaptor.capture());
        assertThat(verificationCaptor.getValue().getMemberUuid()).isEqualTo(authCaptor.getValue().getAuthUuid());
    }

    @Test
    void signUp_throwsWhenVerificationNotSuccessful() {
        IdentityVerificationDomain verification = IdentityVerificationDomain.createRequested(
                "verify-002",
                VerificationPurpose.SIGN_UP
        );

        when(identityVerificationRepositoryPort.findByRequestToken("verify-002"))
                .thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> authService.signUp(signUpRequest("verify-002")))
                .isInstanceOf(IdentityVerificationNotReadyException.class);
    }

    private IdentityVerificationDomain verifiedVerification(String requestToken) {
        return IdentityVerificationDomain.createRequested(requestToken, VerificationPurpose.SIGN_UP)
                .markVerified(
                        VerificationMethod.PASS,
                        "ci-hash-001",
                        Instant.parse("2025-01-01T00:00:00Z")
                );
    }

    private AuthSignUpRequestDto signUpRequest() {
        return signUpRequest("verify-001");
    }

    private AuthSignUpRequestDto signUpRequest(String requestToken) {
        return AuthSignUpRequestDto.builder()
                .requestToken(requestToken)
                .loginId("user01")
                .password("Password1!")
                .email("user@example.com")
                .build();
    }
}
