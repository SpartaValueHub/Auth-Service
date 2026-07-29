package com.unionclass.auth_service.application.port.out;

import com.unionclass.auth_service.domain.model.AuthDomain;

import java.util.Optional;

public interface AuthRepositoryPort {

    boolean existsByLogInId(String logInId);

    boolean existsByEmailAndNotDeleted(String email);

    boolean existsByPhoneAndNotDeleted(String phone);

    Optional<AuthDomain> findByLogInIdAndNotDeleted(String logInId);

    AuthDomain save(AuthDomain authDomain);
}
