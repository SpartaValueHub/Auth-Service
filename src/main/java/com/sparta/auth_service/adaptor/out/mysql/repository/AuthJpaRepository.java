package com.sparta.auth_service.adaptor.out.mysql.repository;

import com.sparta.auth_service.adaptor.out.mysql.entity.AuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** auth 테이블 Spring Data — Application 계층에는 AuthRepositoryPort로만 노출 */
public interface AuthJpaRepository extends JpaRepository<AuthEntity, Long> {

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByIdentityKey(String identityKey);

    Optional<AuthEntity> findByLoginId(String loginId);

    Optional<AuthEntity> findByAuthUuid(String authUuid);
}
