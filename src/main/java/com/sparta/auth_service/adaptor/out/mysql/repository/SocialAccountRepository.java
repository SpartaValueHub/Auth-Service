package com.sparta.auth_service.adaptor.out.mysql.repository;

import com.sparta.auth_service.adaptor.out.mysql.entity.SocialAccountEntity;
import com.sparta.auth_service.domain.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** social_accounts — provider + providerUserId 조합으로 소셜 계정 식별 */
public interface SocialAccountRepository extends JpaRepository<SocialAccountEntity, Long> {

    Optional<SocialAccountEntity> findByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    );

    boolean existsByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
