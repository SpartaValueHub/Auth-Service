package com.sparta.auth_service.application.service;

import com.sparta.auth_service.application.exception.IdentityVerificationFailedException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotFoundException;
import com.sparta.auth_service.application.port.in.IdentityVerificationUseCase;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationConfirmRequestDto;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationResultDto;
import com.sparta.auth_service.application.port.out.FetchIdentityVerificationPort;
import com.sparta.auth_service.application.port.out.IdentityVerificationRepositoryPort;
import com.sparta.auth_service.application.port.out.dto.ExternalIdentityVerificationDto;
import com.sparta.auth_service.domain.model.IdentityVerificationDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 본인인증 이력(상태·purpose·requestToken)만 DB에 저장.
 * CI·PII는 PortOne 조회·API 응답 prefill 용으로만 사용하고 영구 저장하지 않음.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IdentityVerificationService implements IdentityVerificationUseCase {

    private final FetchIdentityVerificationPort fetchIdentityVerificationPort;
    private final IdentityVerificationRepositoryPort identityVerificationRepositoryPort;

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
            // 이미 SUCCESS면 PortOne 재조회 없이 이력·prefill만 반환 (멱등)
            return toResultDto(domain, external);
        }

        IdentityVerificationDomain updated = applyExternalStatus(domain, external);
        IdentityVerificationDomain saved = identityVerificationRepositoryPort.save(updated);
        return toResultDto(saved, external);
    }

    @Override
    public IdentityVerificationResultDto getStatus(String requestToken) {
        if (requestToken == null || requestToken.isBlank()) {
            throw new IllegalArgumentException("requestToken은 필수입니다.");
        }

        String trimmedToken = requestToken.trim();
        IdentityVerificationDomain domain = identityVerificationRepositoryPort.findByRequestToken(trimmedToken)
                .orElseThrow(() -> new IdentityVerificationNotFoundException("본인인증 내역을 찾을 수 없습니다."));

        if (!domain.isSuccessful()) {
            // 미완료 상태 — DB 이력만 반환 (prefill 없음)
            return toResultDto(domain, null);
        }

        // SUCCESS — PortOne에서 prefill용 고객정보 재조회 (DB 미저장)
        ExternalIdentityVerificationDto external = fetchIdentityVerificationPort.fetchByRequestToken(trimmedToken)
                .orElseThrow(() -> new IdentityVerificationNotFoundException("본인인증 내역을 찾을 수 없습니다."));

        return toResultDto(domain, external);
    }

    private IdentityVerificationDomain applyExternalStatus(
            IdentityVerificationDomain domain,
            ExternalIdentityVerificationDto external
    ) {
        return switch (external.getPortOneStatus()) {
            case "VERIFIED" -> verifySuccess(domain, external);
            case "FAILED" -> domain.markFailed();
            case "READY" -> domain.markRequested();
            default -> domain.markFailed();
        };
    }

    private IdentityVerificationDomain verifySuccess(
            IdentityVerificationDomain domain,
            ExternalIdentityVerificationDto external
    ) {
        validateVerifiedCustomer(external);
        return domain.markSuccess();
    }

    private void validateVerifiedCustomer(ExternalIdentityVerificationDto external) {
        if (external.getIdentityKey() == null || external.getIdentityKey().isBlank()) {
            throw new IdentityVerificationFailedException("본인인증 CI를 확인할 수 없습니다.");
        }
        if (external.getMemberName() == null || external.getMemberName().isBlank()
                || external.getPhoneNumber() == null || external.getPhoneNumber().isBlank()
                || external.getBirthdayDate() == null) {
            throw new IdentityVerificationFailedException("본인인증 고객 정보가 불완전합니다.");
        }
    }

    private IdentityVerificationResultDto toResultDto(
            IdentityVerificationDomain domain,
            ExternalIdentityVerificationDto external
    ) {
        // memberName·phone·birthday는 응답 prefill 전용 — Entity/Domain에 저장하지 않음
        return IdentityVerificationResultDto.builder()
                .requestToken(domain.getRequestToken())
                .purpose(domain.getPurpose())
                .status(domain.getStatus())
                .memberName(external != null ? external.getMemberName() : null)
                .phoneNumber(external != null ? external.getPhoneNumber() : null)
                .birthdayDate(external != null ? external.getBirthdayDate() : null)
                .build();
    }
}
