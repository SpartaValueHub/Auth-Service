package com.sparta.auth_service.adaptor.out.mysql.mapper;

import com.sparta.auth_service.adaptor.out.mysql.entity.IdentityVerificationEntity;
import com.sparta.auth_service.domain.model.IdentityVerificationDomain;
import org.springframework.stereotype.Component;

/** requestToken·verificationUuid는 생성 후 변경 없음 */
@Component
public class IdentityVerificationEntityMapper {

    public IdentityVerificationEntity toEntity(IdentityVerificationDomain domain) {
        return IdentityVerificationEntity.builder()
                .verificationUuid(domain.getVerificationUuid())
                .memberUuid(domain.getMemberUuid())
                .purpose(domain.getPurpose())
                .requestToken(domain.getRequestToken())
                .verificationMethod(domain.getVerificationMethod())
                .ciHash(domain.getCiHash())
                .verificationStatus(domain.getVerificationStatus())
                .verifiedAt(domain.getVerifiedAt())
                .build();
    }

    public void updateEntity(IdentityVerificationEntity entity, IdentityVerificationDomain domain) {
        entity.setMemberUuid(domain.getMemberUuid());
        entity.setPurpose(domain.getPurpose());
        entity.setVerificationMethod(domain.getVerificationMethod());
        entity.setCiHash(domain.getCiHash());
        entity.setVerificationStatus(domain.getVerificationStatus());
        entity.setVerifiedAt(domain.getVerifiedAt());
    }

    public IdentityVerificationDomain toDomain(IdentityVerificationEntity entity) {
        return IdentityVerificationDomain.reconstitute(
                entity.getIdentityVerificationId(),
                entity.getVerificationUuid(),
                entity.getMemberUuid(),
                entity.getPurpose(),
                entity.getRequestToken(),
                entity.getVerificationMethod(),
                entity.getCiHash(),
                entity.getVerificationStatus(),
                entity.getVerifiedAt(),
                entity.getCreatedAt()
        );
    }
}
