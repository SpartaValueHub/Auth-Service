package com.unionclass.auth_service.application.port.out;

import com.unionclass.auth_service.domain.model.AuthDomain;

public interface AuthRepositoryPort {

    boolean existsByLogInId(String logInId);

    boolean existsByEmailAndNotDeleted(String email);

    boolean existsByPhoneAndNotDeleted(String phone);

    AuthDomain save(AuthDomain authDomain);
}
