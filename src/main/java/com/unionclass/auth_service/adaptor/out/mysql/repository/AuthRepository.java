package com.unionclass.auth_service.adaptor.out.mysql.repository;

import com.unionclass.auth_service.adaptor.out.mysql.entity.AuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<AuthEntity, Long> {

    boolean existsByLogInId(String logInId);

    boolean existsByEmailAndDeleted(String email, boolean deleted);

    boolean existsByPhoneAndDeleted(String phone, boolean deleted);

    Optional<AuthEntity> findByLogInIdAndDeleted(String logInId, boolean deleted);

    Optional<AuthEntity> findByLogInId(String logInId);

    Optional<AuthEntity> findByUserId(String userId);
}
