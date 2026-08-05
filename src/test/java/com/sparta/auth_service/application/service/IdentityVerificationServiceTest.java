package com.sparta.auth_service.application.service;

import com.sparta.auth_service.application.exception.IdentityVerificationFailedException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotFoundException;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationConfirmRequestDto;
import com.sparta.auth_service.application.port.out.FetchIdentityVerificationPort;
import com.sparta.auth_service.application.port.out.IdentityKeyHashPort;
import com.sparta.auth_service.application.port.out.IdentityVerificationRepositoryPort;
import com.sparta.auth_service.application.port.out.dto.ExternalIdentityVerificationDto;
import com.sparta.auth_service.domain.enums.Gender;
import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.enums.VerificationStatus;
import com.sparta.auth_service.domain.model.IdentityVerificationDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityVerificationServiceTest {

    @Mock
    private FetchIdentityVerificationPort fetchIdentityVerificationPort;

    @Mock
    private IdentityVerificationRepositoryPort identityVerificationRepositoryPort;

    @Mock
    private IdentityKeyHashPort identityKeyHashPort;

    @InjectMocks
    private IdentityVerificationService identityVerificationService;

    @Test
    void confirm_marksSuccessWhenPortOneVerified() {
        ExternalIdentityVerificationDto external = verifiedExternal("verify-001");

        when(fetchIdentityVerificationPort.fetchByRequestToken("verify-001")).thenReturn(Optional.of(external));
        when(identityVerificationRepositoryPort.findByRequestToken("verify-001")).thenReturn(Optional.empty());
        when(identityKeyHashPort.hashForLookup("ci-value-001")).thenReturn("ci-hash-001");
        when(identityVerificationRepositoryPort.save(any(IdentityVerificationDomain.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = identityVerificationService.confirm(confirmRequest("verify-001"));

        assertThat(result.getStatus()).isEqualTo(VerificationStatus.SUCCESS);
        assertThat(result.getMemberName()).isEqualTo("홍길동");
        assertThat(result.getPhoneNumber()).isEqualTo("01012345678");
        assertThat(result.getBirthdayDate()).isNull();
        assertThat(result.getGender()).isEqualTo(Gender.MALE);
        verify(identityVerificationRepositoryPort).save(any(IdentityVerificationDomain.class));
    }

    @Test
    void confirm_persistsCiHashOnly() {
        ExternalIdentityVerificationDto external = verifiedExternal("verify-001");

        when(fetchIdentityVerificationPort.fetchByRequestToken("verify-001")).thenReturn(Optional.of(external));
        when(identityVerificationRepositoryPort.findByRequestToken("verify-001")).thenReturn(Optional.empty());
        when(identityKeyHashPort.hashForLookup("ci-value-001")).thenReturn("ci-hash-001");
        when(identityVerificationRepositoryPort.save(any(IdentityVerificationDomain.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        identityVerificationService.confirm(confirmRequest("verify-001"));

        verify(identityVerificationRepositoryPort).save(
                org.mockito.ArgumentMatchers.argThat(saved ->
                        saved.getVerificationStatus() == VerificationStatus.SUCCESS
                                && saved.getRequestToken().equals("verify-001")
                                && saved.getCiHash().equals("ci-hash-001")
                )
        );
    }

    @Test
    void confirm_throwsWhenPortOneRecordNotFound() {
        when(fetchIdentityVerificationPort.fetchByRequestToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> identityVerificationService.confirm(confirmRequest("missing")))
                .isInstanceOf(IdentityVerificationNotFoundException.class);
    }

    @Test
    void confirm_throwsWhenCiMissingOnVerifiedStatus() {
        ExternalIdentityVerificationDto external = ExternalIdentityVerificationDto.builder()
                .requestToken("verify-002")
                .portOneStatus("VERIFIED")
                .build();

        when(fetchIdentityVerificationPort.fetchByRequestToken("verify-002")).thenReturn(Optional.of(external));
        when(identityVerificationRepositoryPort.findByRequestToken("verify-002")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> identityVerificationService.confirm(confirmRequest("verify-002")))
                .isInstanceOf(IdentityVerificationFailedException.class);

        verify(identityVerificationRepositoryPort, never()).save(any());
    }

    @Test
    void getStatus_returnsPurposeAndStatusOnly() {
        IdentityVerificationDomain stored = IdentityVerificationDomain.createRequested(
                "verify-003",
                VerificationPurpose.SIGN_UP
        ).markVerified(
                com.sparta.auth_service.domain.enums.VerificationMethod.PASS,
                "ci-hash-003",
                java.time.Instant.parse("2025-01-01T00:00:00Z")
        );

        when(identityVerificationRepositoryPort.findByRequestToken("verify-003")).thenReturn(Optional.of(stored));

        var result = identityVerificationService.getStatus("verify-003");

        assertThat(result.getStatus()).isEqualTo(VerificationStatus.SUCCESS);
        assertThat(result.getPurpose()).isEqualTo(VerificationPurpose.SIGN_UP);
        verify(fetchIdentityVerificationPort, never()).fetchByRequestToken(any());
    }

    @Test
    void getStatus_trimsRequestToken() {
        IdentityVerificationDomain stored = IdentityVerificationDomain.createRequested(
                "verify-003",
                VerificationPurpose.SIGN_UP
        );

        when(identityVerificationRepositoryPort.findByRequestToken("verify-003")).thenReturn(Optional.of(stored));

        identityVerificationService.getStatus("  verify-003  ");

        verify(identityVerificationRepositoryPort).findByRequestToken("verify-003");
        verify(fetchIdentityVerificationPort, never()).fetchByRequestToken(any());
    }

    @Test
    void getStatus_returnsStatusOnlyWhenNotSuccessful() {
        IdentityVerificationDomain stored = IdentityVerificationDomain.createRequested(
                "verify-004",
                VerificationPurpose.SIGN_UP
        );

        when(identityVerificationRepositoryPort.findByRequestToken("verify-004")).thenReturn(Optional.of(stored));

        var result = identityVerificationService.getStatus("verify-004");

        assertThat(result.getStatus()).isEqualTo(VerificationStatus.REQUESTED);
        assertThat(result.getPurpose()).isEqualTo(VerificationPurpose.SIGN_UP);
        verify(fetchIdentityVerificationPort, never()).fetchByRequestToken(any());
    }

    private ExternalIdentityVerificationDto verifiedExternal(String requestToken) {
        return ExternalIdentityVerificationDto.builder()
                .requestToken(requestToken)
                .portOneStatus("VERIFIED")
                .identityKey("ci-value-001")
                .memberName("홍길동")
                .phoneNumber("01012345678")
                .birthdayDate(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .build();
    }

    private IdentityVerificationConfirmRequestDto confirmRequest(String identityVerificationId) {
        return IdentityVerificationConfirmRequestDto.builder()
                .identityVerificationId(identityVerificationId)
                .purpose(VerificationPurpose.SIGN_UP)
                .build();
    }
}
