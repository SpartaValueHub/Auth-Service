package com.sparta.auth_service.application.port.out;

import com.sparta.auth_service.domain.model.AuthDomain;

import java.util.Optional;

public interface AuthRepositoryPort {

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<AuthDomain> findByLoginId(String loginId);

    Optional<AuthDomain> findByAuthUuid(String authUuid);

    AuthDomain save(AuthDomain authDomain);
}
