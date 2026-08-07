package com.sparta.auth_service.adaptor.out.mysql;

import com.sparta.auth_service.adaptor.out.mysql.entity.SignupIdentityClaimEntity;
import com.sparta.auth_service.adaptor.out.mysql.repository.SignupIdentityClaimJpaRepository;
import com.sparta.auth_service.application.exception.DuplicateResourceException;
import com.sparta.auth_service.application.port.out.SignupIdentityClaimPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignupIdentityClaimAdapter implements SignupIdentityClaimPort {

    private final SignupIdentityClaimJpaRepository repository;

    @Override
    public void claim(String ciHash, String authUuid) {
        try {
            repository.saveAndFlush(SignupIdentityClaimEntity.create(ciHash, authUuid));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException(
                    "AUTH_DUPLICATE_IDENTITY",
                    "이미 가입된 본인인증 정보입니다."
            );
        }
    }
}
