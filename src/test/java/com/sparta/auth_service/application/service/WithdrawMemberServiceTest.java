package com.sparta.auth_service.application.service;

import com.sparta.auth_service.application.exception.AuthIdentityMismatchException;
import com.sparta.auth_service.application.exception.AuthNotFoundException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotReadyException;
import com.sparta.auth_service.application.exception.MemberNotActiveException;
import com.sparta.auth_service.application.port.in.dto.WithdrawMemberRequestDto;
import com.sparta.auth_service.application.port.out.AuthRepositoryPort;
import com.sparta.auth_service.application.port.out.IdentityVerificationRepositoryPort;
import com.sparta.auth_service.application.port.out.SessionInvalidationPort;
import com.sparta.auth_service.domain.enums.Gender;
import com.sparta.auth_service.domain.enums.MemberStatus;
import com.sparta.auth_service.domain.enums.VerificationMethod;
import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.enums.VerificationStatus;
import com.sparta.auth_service.domain.model.AuthDomain;
import com.sparta.auth_service.domain.model.IdentityVerificationDomain;
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
class WithdrawMemberServiceTest {

    private static final String AUTH_UUID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String REQUEST_TOKEN = "withdraw-token-001";
    private static final String CI_HASH = "ci-hash-same";
    private static final Instant NOW = Instant.parse("2026-08-04T08:00:00Z");

    @Mock
    private AuthRepositoryPort authRepositoryPort;
    @Mock
    private IdentityVerificationRepositoryPort identityVerificationRepositoryPort;
    @Mock
    private SessionInvalidationPort sessionInvalidationPort;

    @InjectMocks
    private WithdrawMemberService withdrawMemberService;

    @Test
    void withdraw_success_matchesCi_savesWithdrawn_andRevokesSession() {
        when(authRepositoryPort.findByAuthUuid(AUTH_UUID)).thenReturn(Optional.of(activeAuth()));
        when(identityVerificationRepositoryPort.findByRequestToken(REQUEST_TOKEN))
                .thenReturn(Optional.of(withdrawalVerification(CI_HASH, null)));
        when(identityVerificationRepositoryPort.findSignUpLinkedByMemberUuid(AUTH_UUID))
                .thenReturn(Optional.of(signUpLinked(CI_HASH)));
        when(authRepositoryPort.save(any(AuthDomain.class))).thenAnswer(inv -> inv.getArgument(0));
        when(identityVerificationRepositoryPort.save(any(IdentityVerificationDomain.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        withdrawMemberService.withdraw(request());

        ArgumentCaptor<AuthDomain> authCaptor = ArgumentCaptor.forClass(AuthDomain.class);
        verify(authRepositoryPort).save(authCaptor.capture());
        assertThat(authCaptor.getValue().isWithdrawn()).isTrue();

        ArgumentCaptor<IdentityVerificationDomain> verificationCaptor =
                ArgumentCaptor.forClass(IdentityVerificationDomain.class);
        verify(identityVerificationRepositoryPort).save(verificationCaptor.capture());
        assertThat(verificationCaptor.getValue().isLinkedToMember(AUTH_UUID)).isTrue();

        verify(sessionInvalidationPort).revokeAllSessions(AUTH_UUID);
    }

    @Test
    void withdraw_rejectsCiMismatch() {
        when(authRepositoryPort.findByAuthUuid(AUTH_UUID)).thenReturn(Optional.of(activeAuth()));
        when(identityVerificationRepositoryPort.findByRequestToken(REQUEST_TOKEN))
                .thenReturn(Optional.of(withdrawalVerification("ci-other", null)));
        when(identityVerificationRepositoryPort.findSignUpLinkedByMemberUuid(AUTH_UUID))
                .thenReturn(Optional.of(signUpLinked(CI_HASH)));

        assertThatThrownBy(() -> withdrawMemberService.withdraw(request()))
                .isInstanceOf(AuthIdentityMismatchException.class);

        verify(authRepositoryPort, never()).save(any());
        verify(sessionInvalidationPort, never()).revokeAllSessions(any());
    }

    @Test
    void withdraw_rejectsWhenAuthMissing() {
        when(authRepositoryPort.findByAuthUuid(AUTH_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawMemberService.withdraw(request()))
                .isInstanceOf(AuthNotFoundException.class);
    }

    @Test
    void withdraw_rejectsWhenWithdrawalVerificationNotReady() {
        when(authRepositoryPort.findByAuthUuid(AUTH_UUID)).thenReturn(Optional.of(activeAuth()));
        when(identityVerificationRepositoryPort.findByRequestToken(REQUEST_TOKEN))
                .thenReturn(Optional.of(IdentityVerificationDomain.createRequested(
                        REQUEST_TOKEN, VerificationPurpose.WITHDRAWAL)));

        assertThatThrownBy(() -> withdrawMemberService.withdraw(request()))
                .isInstanceOf(IdentityVerificationNotReadyException.class);
    }

    @Test
    void withdraw_rejectsSuspendedAccount() {
        when(authRepositoryPort.findByAuthUuid(AUTH_UUID)).thenReturn(Optional.of(authWithStatus(MemberStatus.SUSPENDED)));
        when(identityVerificationRepositoryPort.findByRequestToken(REQUEST_TOKEN))
                .thenReturn(Optional.of(withdrawalVerification(CI_HASH, null)));
        when(identityVerificationRepositoryPort.findSignUpLinkedByMemberUuid(AUTH_UUID))
                .thenReturn(Optional.of(signUpLinked(CI_HASH)));

        assertThatThrownBy(() -> withdrawMemberService.withdraw(request()))
                .isInstanceOf(MemberNotActiveException.class);

        verify(authRepositoryPort, never()).save(any());
        verify(sessionInvalidationPort, never()).revokeAllSessions(any());
    }

    @Test
    void withdraw_alreadyWithdrawn_isIdempotentAndRevokesSession() {
        when(authRepositoryPort.findByAuthUuid(AUTH_UUID)).thenReturn(Optional.of(authWithStatus(MemberStatus.WITHDRAWN)));
        when(identityVerificationRepositoryPort.findByRequestToken(REQUEST_TOKEN))
                .thenReturn(Optional.of(withdrawalVerification(CI_HASH, AUTH_UUID)));
        when(identityVerificationRepositoryPort.findSignUpLinkedByMemberUuid(AUTH_UUID))
                .thenReturn(Optional.of(signUpLinked(CI_HASH)));

        withdrawMemberService.withdraw(request());

        verify(authRepositoryPort, never()).save(any());
        verify(identityVerificationRepositoryPort, never()).save(any());
        verify(sessionInvalidationPort).revokeAllSessions(AUTH_UUID);
    }

    private static WithdrawMemberRequestDto request() {
        return WithdrawMemberRequestDto.builder()
                .authUuid(AUTH_UUID)
                .requestToken(REQUEST_TOKEN)
                .build();
    }

    private static AuthDomain activeAuth() {
        return authWithStatus(MemberStatus.ACTIVE);
    }

    private static AuthDomain authWithStatus(MemberStatus status) {
        return AuthDomain.reconstitute(
                AUTH_UUID,
                "user01",
                "홍길동",
                LocalDate.of(1990, 1, 1),
                "01012345678",
                Gender.MALE,
                "user@example.com",
                "hash",
                NOW,
                status,
                NOW,
                NOW
        );
    }

    private static IdentityVerificationDomain withdrawalVerification(String ciHash, String memberUuid) {
        return IdentityVerificationDomain.reconstitute(
                2L,
                "withdraw-uuid",
                memberUuid,
                VerificationPurpose.WITHDRAWAL,
                REQUEST_TOKEN,
                VerificationMethod.PASS,
                ciHash,
                VerificationStatus.SUCCESS,
                NOW,
                NOW
        );
    }

    private static IdentityVerificationDomain signUpLinked(String ciHash) {
        return IdentityVerificationDomain.reconstitute(
                1L,
                "signup-uuid",
                AUTH_UUID,
                VerificationPurpose.SIGN_UP,
                "signup-token",
                VerificationMethod.PASS,
                ciHash,
                VerificationStatus.SUCCESS,
                NOW,
                NOW
        );
    }
}
