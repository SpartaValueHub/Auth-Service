package com.sparta.auth_service.adaptor.out.mysql;

import com.sparta.auth_service.adaptor.out.mysql.entity.AuthEntity;
import com.sparta.auth_service.adaptor.out.mysql.entity.SocialAccountEntity;
import com.sparta.auth_service.adaptor.out.mysql.mapper.SocialAccountEntityMapper;
import com.sparta.auth_service.adaptor.out.mysql.repository.AuthJpaRepository;
import com.sparta.auth_service.adaptor.out.mysql.repository.SocialAccountRepository;
import com.sparta.auth_service.application.port.out.SocialAccountRepositoryPort;
import com.sparta.auth_service.domain.enums.SocialProvider;
import com.sparta.auth_service.domain.model.SocialAccountDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** SocialAccountDomain ↔ Entity — authUuid→auth_id 변환 후 저장·조회 */
@Component
@RequiredArgsConstructor
public class SocialAccountRepositoryAdapter implements SocialAccountRepositoryPort {

    private final SocialAccountRepository socialAccountRepository;
    private final AuthJpaRepository authJpaRepository;
    private final SocialAccountEntityMapper socialAccountEntityMapper;

    @Override
    public SocialAccountDomain save(SocialAccountDomain domain) {
        // Domain authUuid → auth.auth_id FK 조회 (Entity에는 authUuid 컬럼 없음)
        AuthEntity authEntity = authJpaRepository.findByAuthUuid(domain.getAuthUuid())
                .orElseThrow(() -> new IllegalArgumentException("authUuid에 해당하는 회원이 없습니다."));

        SocialAccountEntity entity = socialAccountEntityMapper.toEntity(domain, authEntity.getAuthId());
        SocialAccountEntity saved = socialAccountRepository.save(entity);
        return socialAccountEntityMapper.toDomain(saved, domain.getAuthUuid());
    }

    @Override
    public Optional<SocialAccountDomain> findByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    ) {
        return socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                // Entity auth_id → auth.auth_uuid 역조회 후 Domain에 authUuid 부여
                .flatMap(entity -> authJpaRepository.findById(entity.getAuthId())
                        .map(auth -> socialAccountEntityMapper.toDomain(entity, auth.getAuthUuid())));
    }

    @Override
    public boolean existsByProviderAndProviderUserId(SocialProvider provider, String providerUserId) {
        return socialAccountRepository.existsByProviderAndProviderUserId(provider, providerUserId);
    }
}
