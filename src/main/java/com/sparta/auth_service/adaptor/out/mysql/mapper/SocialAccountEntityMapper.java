package com.sparta.auth_service.adaptor.out.mysql.mapper;

import com.sparta.auth_service.adaptor.out.mysql.entity.SocialAccountEntity;
import com.sparta.auth_service.domain.model.SocialAccountDomain;
import org.springframework.stereotype.Component;

/** Entity는 auth_id(FK), Domain은 authUuid — Adapter가 authUuid→authId 조회 후 toEntity 호출 */
@Component
public class SocialAccountEntityMapper {

    public SocialAccountEntity toEntity(SocialAccountDomain domain, Long authId) {
        return SocialAccountEntity.builder()
                .authId(authId)
                .provider(domain.getProvider())
                .providerUserId(domain.getProviderUserId())
                .providerEmail(domain.getProviderEmail())
                .linkedAt(domain.getLinkedAt())
                .build();
    }

    public SocialAccountDomain toDomain(SocialAccountEntity entity, String authUuid) {
        return SocialAccountDomain.reconstitute(
                entity.getSocialAccountId(),
                authUuid,
                entity.getProvider(),
                entity.getProviderUserId(),
                entity.getProviderEmail(),
                entity.getLinkedAt(),
                entity.getCreatedAt()
        );
    }
}
