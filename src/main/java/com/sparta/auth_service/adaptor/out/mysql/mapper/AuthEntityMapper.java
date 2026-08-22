package com.sparta.auth_service.adaptor.out.mysql.mapper;

import com.sparta.auth_service.adaptor.out.mysql.entity.AuthEntity;
import com.sparta.auth_service.domain.model.AuthDomain;
import org.springframework.stereotype.Component;

/** 신규 가입은 toEntity. 탈퇴(WITHDRAWN)만 loginId 포함 갱신, 그 외는 loginId 불변 */
@Component
public class AuthEntityMapper {

    public AuthEntity toEntity(AuthDomain domain) {
        return AuthEntity.builder()
                .authUuid(domain.getAuthUuid())
                .loginId(domain.getLoginId())
                .memberName(domain.getMemberName())
                .birthdayDate(domain.getBirthdayDate())
                .phoneNumber(domain.getPhoneNumber())
                .gender(domain.getGender())
                .email(domain.getEmail())
                .passwordHash(domain.getPasswordHash())
                .passwordChangedAt(domain.getPasswordChangedAt())
                .memberStatus(domain.getMemberStatus())
                .build();
    }

    public void updateEntity(AuthEntity entity, AuthDomain domain) {
        if (domain.isWithdrawn()) {
            entity.applyWithdrawal(
                    domain.getLoginId(),
                    domain.getPhoneNumber(),
                    domain.getEmail(),
                    domain.getMemberStatus()
            );
            return;
        }
        entity.updateProfile(
                domain.getMemberName(),
                domain.getBirthdayDate(),
                domain.getPhoneNumber(),
                domain.getGender(),
                domain.getEmail(),
                domain.getPasswordHash(),
                domain.getPasswordChangedAt(),
                domain.getMemberStatus()
        );
    }

    public AuthDomain toDomain(AuthEntity entity) {
        return AuthDomain.reconstitute(
                entity.getAuthUuid(),
                entity.getLoginId(),
                entity.getMemberName(),
                entity.getBirthdayDate(),
                entity.getPhoneNumber(),
                entity.getGender(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getPasswordChangedAt(),
                entity.getMemberStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
