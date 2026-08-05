package com.sparta.auth_service.application.service;

import com.sparta.auth_service.application.exception.IdentityVerificationFailedException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotFoundException;
import com.sparta.auth_service.application.port.in.IdentityVerificationUseCase;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationConfirmRequestDto;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationResultDto;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationStatusResultDto;
import com.sparta.auth_service.application.port.out.FetchIdentityVerificationPort;
import com.sparta.auth_service.application.port.out.IdentityVerificationRepositoryPort;
import com.sparta.auth_service.application.port.out.IdentityKeyHashPort;
import com.sparta.auth_service.application.port.out.dto.ExternalIdentityVerificationDto;
import com.sparta.auth_service.domain.enums.VerificationMethod;
import com.sparta.auth_service.domain.model.IdentityVerificationDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 본인인증 이력 저장 — CI는 identity_verifications.ci_hash(HMAC)만 저장.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IdentityVerificationService implements IdentityVerificationUseCase {

    private final FetchIdentityVerificationPort fetchIdentityVerificationPort;
    private final IdentityVerificationRepositoryPort identityVerificationRepositoryPort;
    private final IdentityKeyHashPort identityKeyHashPort;

    @Override
    @Transactional
    public IdentityVerificationResultDto confirm(IdentityVerificationConfirmRequestDto requestDto) {
        if (requestDto.getIdentityVerificationId() == null || requestDto.getIdentityVerificationId().isBlank()) {
            throw new IllegalArgumentException("identityVerificationId는 필수입니다.");
        }
        if (requestDto.getPurpose() == null) {
            throw new IllegalArgumentException("purpose는 필수입니다.");
        }

        String requestToken = requestDto.getIdentityVerificationId().trim();
        ExternalIdentityVerificationDto external = fetchIdentityVerificationPort.fetchByRequestToken(requestToken)
                .orElseThrow(() -> new IdentityVerificationNotFoundException("본인인증 내역을 찾을 수 없습니다."));

        IdentityVerificationDomain domain = identityVerificationRepositoryPort.findByRequestToken(requestToken)
                .orElseGet(() -> IdentityVerificationDomain.createRequested(requestToken, requestDto.getPurpose()));

        if (domain.getPurpose() != requestDto.getPurpose()) {
            throw new IllegalArgumentException("본인인증 purpose가 일치하지 않습니다.");
        }

        if (domain.isSuccessful()) {
            return toStatusDto(domain);
        }

        IdentityVerificationDomain updated = applyExternalStatus(domain, external);
        IdentityVerificationDomain saved = identityVerificationRepositoryPort.save(updated);
        return toConfirmDto(saved, external);
    }

    @Override
    public IdentityVerificationStatusResultDto getStatus(String requestToken) {
        IdentityVerificationDomain domain = identityVerificationRepositoryPort.findByRequestToken(requestToken.trim())
                .orElseThrow(() -> new IdentityVerificationNotFoundException("본인인증 내역을 찾을 수 없습니다."));

        return IdentityVerificationStatusResultDto.builder()
                .purpose(domain.getPurpose())
                .status(domain.getVerificationStatus())
                .build();
    }

    private IdentityVerificationDomain applyExternalStatus(
            IdentityVerificationDomain domain,
            ExternalIdentityVerificationDto external
    ) {
        return switch (external.getPortOneStatus()) {
            case "VERIFIED" -> verifySuccess(domain, external);
            case "FAILED" -> domain.markFailed();
            // READY: PortOne 진행 중 — REQUESTED 유지. 재인증은 새 requestToken으로 createRequested.
            case "READY" -> domain;
            default -> domain.markFailed();
        };
    }

    private IdentityVerificationDomain verifySuccess(
            IdentityVerificationDomain domain,
            ExternalIdentityVerificationDto external
    ) {
        validateVerifiedCustomer(external);
        String ciHash = identityKeyHashPort.hashForLookup(external.getIdentityKey());
        VerificationMethod method = resolveVerificationMethod(external);
        return domain.markVerified(method, ciHash, Instant.now());
    }

    private VerificationMethod resolveVerificationMethod(ExternalIdentityVerificationDto external) {
        if (external.getVerificationMethod() != null) {
            return external.getVerificationMethod();
        }
        return VerificationMethod.PASS;
    }

    private void validateVerifiedCustomer(ExternalIdentityVerificationDto external) {
        if (external.getIdentityKey() == null || external.getIdentityKey().isBlank()) {
            throw new IdentityVerificationFailedException("본인인증 CI를 확인할 수 없습니다.");
        }
        if (external.getMemberName() == null || external.getMemberName().isBlank()
                || external.getPhoneNumber() == null || external.getPhoneNumber().isBlank()
                || external.getBirthdayDate() == null
                || external.getGender() == null) {
            throw new IdentityVerificationFailedException("본인인증 고객 정보가 불완전합니다.");
        }
    }

    /** confirm idempotent — 이미 SUCCESS면 PII·PortOne 재조회 없이 status만 반환 */
    private IdentityVerificationResultDto toStatusDto(IdentityVerificationDomain domain) {
        return IdentityVerificationResultDto.builder()
                .requestToken(domain.getRequestToken())
                .purpose(domain.getPurpose())
                .status(domain.getVerificationStatus())
                .build();
    }

    /** confirm — 가입 prefill용 최소 PII만 포함 */
    private IdentityVerificationResultDto toConfirmDto(
            IdentityVerificationDomain domain,
            ExternalIdentityVerificationDto external
    ) {
        IdentityVerificationResultDto.IdentityVerificationResultDtoBuilder builder = IdentityVerificationResultDto.builder()
                .requestToken(domain.getRequestToken())
                .purpose(domain.getPurpose())
                .status(domain.getVerificationStatus());

        if (domain.isSuccessful() && external != null) {
            builder.memberName(external.getMemberName())
                    .phoneNumber(external.getPhoneNumber())
                    .gender(external.getGender());
        }

        return builder.build();
    }
}
