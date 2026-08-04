package com.sparta.auth_service.adaptor.out.mysql.mapper;

import com.sparta.auth_service.adaptor.out.mysql.entity.AuthEntity;
import com.sparta.auth_service.domain.model.AuthDomain;
import org.springframework.stereotype.Component;

/** authUuid·loginId 등 불변 필드는 updateEntity에서 제외 — 신규 가입 시 toEntity만 사용 */
@Component
public class AuthEntityMapper {

    public AuthEntity toEntity(AuthDomain domain) {
        return AuthEntity.builder()
                .authUuid(domain.getAuthUuid())
                .loginId(domain.getLoginId())
                .memberName(domain.getMemberName())
                .birthdayDate(domain.getBirthdayDate())
                .phoneNumber(domain.getPhoneNumber())
                .email(domain.getEmail())
                .identityKey(domain.getIdentityKey())
                .passwordHash(domain.getPasswordHash())
                .passwordChangedAt(domain.getPasswordChangedAt())
                .loginFailCount(domain.getLoginFailCount())
                .lockedUntil(domain.getLockedUntil())
                .build();
    }

    public void updateEntity(AuthEntity entity, AuthDomain domain) {
        entity.setMemberName(domain.getMemberName());
        entity.setBirthdayDate(domain.getBirthdayDate());
        entity.setPhoneNumber(domain.getPhoneNumber());
        entity.setEmail(domain.getEmail());
        entity.setIdentityKey(domain.getIdentityKey());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setPasswordChangedAt(domain.getPasswordChangedAt());
        entity.setLoginFailCount(domain.getLoginFailCount());
        entity.setLockedUntil(domain.getLockedUntil());
    }

    public AuthDomain toDomain(AuthEntity entity) {
        return AuthDomain.reconstitute(
                entity.getAuthUuid(),
                entity.getLoginId(),
                entity.getMemberName(),
                entity.getBirthdayDate(),
                entity.getPhoneNumber(),
                entity.getEmail(),
                entity.getIdentityKey(),
                entity.getPasswordHash(),
                entity.getPasswordChangedAt(),
                entity.getLoginFailCount(),
                entity.getLockedUntil(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
