package com.sparta.auth_service.adaptor.out.mysql.repository;

import com.sparta.auth_service.adaptor.out.mysql.entity.IdentityVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** identity_verifications — requestToken unique, PII 컬럼 없음 */
public interface IdentityVerificationRepository extends JpaRepository<IdentityVerificationEntity, Long> {

    Optional<IdentityVerificationEntity> findByRequestToken(String requestToken);
}
