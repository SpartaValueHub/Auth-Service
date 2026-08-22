package com.sparta.auth_service.application.port.out;

import com.sparta.auth_service.domain.model.IdentityVerificationDomain;

import java.util.Optional;

/** 본인인증 이력 영속화 — requestToken·verificationUuid unique */
public interface IdentityVerificationRepositoryPort {

    IdentityVerificationDomain save(IdentityVerificationDomain domain);

    Optional<IdentityVerificationDomain> findByRequestToken(String requestToken);

    /** 가입 시 memberUuid(=authUuid)가 연결된 SIGN_UP SUCCESS 이력 — 탈퇴 CI 매칭용 */
    Optional<IdentityVerificationDomain> findSignUpLinkedByMemberUuid(String memberUuid);
}
