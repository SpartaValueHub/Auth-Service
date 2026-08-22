package com.sparta.auth_service.application.service;

import com.sparta.auth_service.application.exception.DuplicateResourceException;
import com.sparta.auth_service.application.exception.SecurityStoreUnavailableException;
import com.sparta.auth_service.application.port.out.AuthRepositoryPort;
import com.sparta.auth_service.application.port.out.IdentityVerificationRepositoryPort;
import com.sparta.auth_service.application.port.out.SignupIdentityClaimPort;
import com.sparta.auth_service.domain.enums.Gender;
import com.sparta.auth_service.domain.enums.VerificationMethod;
import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.model.AuthDomain;
import com.sparta.auth_service.domain.model.IdentityVerificationDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupPersistenceServiceTest {

    @Mock AuthRepositoryPort authRepositoryPort;
    @Mock IdentityVerificationRepositoryPort identityVerificationRepositoryPort;
    @Mock SignupIdentityClaimPort signupIdentityClaimPort;
    @Mock com.sparta.auth_service.application.port.out.SignupCompletionTokenPort signupCompletionTokenPort;
    @InjectMocks SignupPersistenceService service;

    @Test
    void persist_claimsIdentityBeforeSavingAuthAndLinksVerification() {
        IdentityVerificationDomain verification = verified();
        AuthDomain auth = auth();
        when(identityVerificationRepositoryPort.findByRequestToken("verify-1"))
                .thenReturn(Optional.of(verification));
        when(authRepositoryPort.save(auth)).thenReturn(auth);

        service.persist("verify-1", "ci-hash", auth, "completion-jti", 120L);

        InOrder order = inOrder(
                signupIdentityClaimPort,
                authRepositoryPort,
                identityVerificationRepositoryPort,
                signupCompletionTokenPort
        );
        order.verify(signupIdentityClaimPort).claim("ci-hash", auth.getAuthUuid());
        order.verify(authRepositoryPort).save(auth);
        order.verify(identityVerificationRepositoryPort).save(any(IdentityVerificationDomain.class));
        order.verify(signupCompletionTokenPort).save(auth.getAuthUuid(), "completion-jti", 120L);
    }

    @Test
    void persist_stopsBeforeClaimWhenIdentityAlreadyExists() {
        AuthDomain auth = auth();
        when(identityVerificationRepositoryPort.findByRequestToken("verify-1"))
                .thenReturn(Optional.of(verified()));
        when(signupIdentityClaimPort.existsByCiHash("ci-hash")).thenReturn(true);

        assertThatThrownBy(() -> service.persist(
                "verify-1", "ci-hash", auth, "completion-jti", 120L
        ))
                .isInstanceOf(DuplicateResourceException.class)
                .extracting("code")
                .isEqualTo("AUTH_DUPLICATE_IDENTITY");

        verify(signupIdentityClaimPort, never()).claim(any(), any());
        verify(authRepositoryPort, never()).save(any());
    }

    @Test
    void persist_stopsBeforeAuthSaveWhenIdentityClaimConflicts() {
        AuthDomain auth = auth();
        when(identityVerificationRepositoryPort.findByRequestToken("verify-1"))
                .thenReturn(Optional.of(verified()));
        org.mockito.Mockito.doThrow(new DuplicateResourceException("AUTH_DUPLICATE_IDENTITY", "duplicate"))
                .when(signupIdentityClaimPort).claim("ci-hash", auth.getAuthUuid());

        assertThatThrownBy(() -> service.persist(
                "verify-1", "ci-hash", auth, "completion-jti", 120L
        ))
                .isInstanceOf(DuplicateResourceException.class)
                .extracting("code")
                .isEqualTo("AUTH_DUPLICATE_IDENTITY");

        verify(authRepositoryPort, never()).save(any());
        verify(identityVerificationRepositoryPort, never()).save(any());
    }

    @Test
    void persist_propagatesSecurityStoreFailureSoTransactionCanRollBack() {
        AuthDomain auth = auth();
        when(identityVerificationRepositoryPort.findByRequestToken("verify-1"))
                .thenReturn(Optional.of(verified()));
        when(authRepositoryPort.save(auth)).thenReturn(auth);
        org.mockito.Mockito.doThrow(new SecurityStoreUnavailableException(new RuntimeException("redis down")))
                .when(signupCompletionTokenPort)
                .save(auth.getAuthUuid(), "completion-jti", 120L);

        assertThatThrownBy(() -> service.persist(
                "verify-1", "ci-hash", auth, "completion-jti", 120L
        )).isInstanceOf(SecurityStoreUnavailableException.class);

        verify(signupCompletionTokenPort)
                .save(auth.getAuthUuid(), "completion-jti", 120L);
    }

    private IdentityVerificationDomain verified() {
        return IdentityVerificationDomain.createRequested("verify-1", VerificationPurpose.SIGN_UP)
                .markVerified(VerificationMethod.PASS, "ci-hash", Instant.parse("2026-01-01T00:00:00Z"));
    }

    private AuthDomain auth() {
        return AuthDomain.createSignUp(
                "user01", "encoded-password", "user@example.com", "홍길동",
                LocalDate.of(1990, 1, 1), "01012345678", Gender.MALE
        );
    }
}
