package com.sparta.auth_service.application.port.out;

import com.sparta.auth_service.domain.enums.SocialProvider;
import com.sparta.auth_service.domain.model.SocialAccountDomain;

import java.util.Optional;

/** 소셜 계정 연동 영속화 — provider + providerUserId로 조회·중복 확인 */
public interface SocialAccountRepositoryPort {

    SocialAccountDomain save(SocialAccountDomain domain);

    Optional<SocialAccountDomain> findByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    );

    boolean existsByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
