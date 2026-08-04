package com.sparta.auth_service.adaptor.out.mysql;

import com.sparta.auth_service.adaptor.out.mysql.entity.IdentityVerificationEntity;
import com.sparta.auth_service.adaptor.out.mysql.mapper.IdentityVerificationEntityMapper;
import com.sparta.auth_service.adaptor.out.mysql.repository.IdentityVerificationRepository;
import com.sparta.auth_service.application.port.out.IdentityVerificationRepositoryPort;
import com.sparta.auth_service.domain.model.IdentityVerificationDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** identity_verifications — requestToken 기준 upsert, PII 컬럼 없음 */
@Component
@RequiredArgsConstructor
public class IdentityVerificationRepositoryAdapter implements IdentityVerificationRepositoryPort {

    private final IdentityVerificationRepository identityVerificationRepository;
    private final IdentityVerificationEntityMapper identityVerificationEntityMapper;

    @Override
    public IdentityVerificationDomain save(IdentityVerificationDomain domain) {
        IdentityVerificationEntity entity = identityVerificationRepository.findByRequestToken(domain.getRequestToken())
                .map(existing -> {
                    identityVerificationEntityMapper.updateEntity(existing, domain);
                    return existing;
                })
                .orElseGet(() -> identityVerificationEntityMapper.toEntity(domain));
        IdentityVerificationEntity saved = identityVerificationRepository.save(entity);
        return identityVerificationEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<IdentityVerificationDomain> findByRequestToken(String requestToken) {
        return identityVerificationRepository.findByRequestToken(requestToken)
                .map(identityVerificationEntityMapper::toDomain);
    }
}
