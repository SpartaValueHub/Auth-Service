package com.sparta.auth_service.application.port.out;

import com.sparta.auth_service.domain.model.AuthDomain;

import java.util.Optional;

/** auth 계정 영속화 — loginId·email·identityKey(CI) 중복은 DB unique + Application 검증 */
public interface AuthRepositoryPort {

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByIdentityKey(String identityKey);

    Optional<AuthDomain> findByLoginId(String loginId);

    Optional<AuthDomain> findByAuthUuid(String authUuid);

    AuthDomain save(AuthDomain authDomain);
}
