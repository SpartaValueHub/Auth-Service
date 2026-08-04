package com.sparta.auth_service.application.port.out;

import com.sparta.auth_service.domain.model.IdentityVerificationDomain;

import java.util.Optional;

/** 본인인증 이력 영속화 — requestToken unique */
public interface IdentityVerificationRepositoryPort {

    IdentityVerificationDomain save(IdentityVerificationDomain domain);

    Optional<IdentityVerificationDomain> findByRequestToken(String requestToken);
}
