package com.sparta.auth_service.adaptor.out.mysql.repository;

import com.sparta.auth_service.adaptor.out.mysql.entity.IdentityVerificationEntity;
import com.sparta.auth_service.domain.enums.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** identity_verifications — requestToken·verificationUuid unique, ci_hash는 UNIQUE 없음 */
public interface IdentityVerificationRepository extends JpaRepository<IdentityVerificationEntity, Long> {

    Optional<IdentityVerificationEntity> findByRequestToken(String requestToken);

    boolean existsByCiHashAndPurposeAndMemberUuidIsNotNull(String ciHash, VerificationPurpose purpose);
}
