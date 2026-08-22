package com.sparta.auth_service.application.port.out;

import com.sparta.auth_service.domain.model.IdentityVerificationDomain;

import java.util.Optional;

/** 본인인증 이력 영속화 — requestToken·verificationUuid unique */
public interface IdentityVerificationRepositoryPort {

    IdentityVerificationDomain save(IdentityVerificationDomain domain);

    Optional<IdentityVerificationDomain> findByRequestToken(String requestToken);

    /**
     * SIGN_UP 완료 후 memberUuid가 연결된 동일 CI(ci_hash) 이력 존재 여부.
     * 사전 검사(pre-check)만 제공하며, ci_hash UNIQUE 부재로 동시 가입 race를 완전히 방지하지 못한다.
     */
    boolean existsSignUpLinkedByCiHash(String ciHash);

    /** 가입 시 memberUuid(=authUuid)가 연결된 SIGN_UP SUCCESS 이력 — 탈퇴 CI 매칭용 */
    Optional<IdentityVerificationDomain> findSignUpLinkedByMemberUuid(String memberUuid);
}
