package com.sparta.auth_service.adaptor.out.mysql.mapper;

import com.sparta.auth_service.adaptor.out.mysql.entity.IdentityVerificationEntity;
import com.sparta.auth_service.domain.model.IdentityVerificationDomain;
import org.springframework.stereotype.Component;

/** requestToken은 생성 후 변경 없음 — status·memberUuid만 updateEntity로 갱신 */
@Component
public class IdentityVerificationEntityMapper {

    public IdentityVerificationEntity toEntity(IdentityVerificationDomain domain) {
        return IdentityVerificationEntity.builder()
                .memberUuid(domain.getMemberUuid())
                .purpose(domain.getPurpose())
                .requestToken(domain.getRequestToken())
                .status(domain.getStatus())
                .build();
    }

    public void updateEntity(IdentityVerificationEntity entity, IdentityVerificationDomain domain) {
        entity.setMemberUuid(domain.getMemberUuid());
        entity.setPurpose(domain.getPurpose());
        entity.setStatus(domain.getStatus());
    }

    public IdentityVerificationDomain toDomain(IdentityVerificationEntity entity) {
        return IdentityVerificationDomain.reconstitute(
                entity.getIdentityVerificationId(),
                entity.getMemberUuid(),
                entity.getPurpose(),
                entity.getRequestToken(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
