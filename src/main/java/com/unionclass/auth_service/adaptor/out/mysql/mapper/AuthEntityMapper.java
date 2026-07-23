package com.unionclass.auth_service.adaptor.out.mysql.mapper;

import com.unionclass.auth_service.adaptor.out.mysql.entity.AuthEntity;
import com.unionclass.auth_service.domain.model.AuthDomain;
import org.springframework.stereotype.Component;

@Component
public class AuthEntityMapper {

    public AuthEntity toEntity(AuthDomain domain) {
        return AuthEntity.builder()
                .userId(domain.getUserId())
                .logInId(domain.getLogInId())
                .password(domain.getPassword())
                .email(domain.getEmail())
                .name(domain.getName())
                .phone(domain.getPhone())
                .deleted(domain.isDeleted())
                .build();
    }

    public AuthDomain toDomain(AuthEntity entity) {
        return AuthDomain.reconstitute(
                entity.getUserId(),
                entity.getLogInId(),
                entity.getPassword(),
                entity.getEmail(),
                entity.getName(),
                entity.getPhone(),
                entity.isDeleted()
        );
    }
}
