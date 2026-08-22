package com.sparta.auth_service.adaptor.out.mysql.repository;

import com.sparta.auth_service.adaptor.out.mysql.entity.SignupIdentityClaimEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignupIdentityClaimJpaRepository extends JpaRepository<SignupIdentityClaimEntity, Long> {

    boolean existsByCiHash(String ciHash);

    void deleteByAuthUuid(String authUuid);
}
