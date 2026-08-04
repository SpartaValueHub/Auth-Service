package com.sparta.auth_service.adaptor.out.mysql;

import com.sparta.auth_service.adaptor.out.mysql.entity.AuthEntity;
import com.sparta.auth_service.adaptor.out.mysql.mapper.AuthEntityMapper;
import com.sparta.auth_service.adaptor.out.mysql.repository.AuthJpaRepository;
import com.sparta.auth_service.application.port.out.AuthRepositoryPort;
import com.sparta.auth_service.domain.model.AuthDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** AuthDomain ↔ AuthEntity — authUuid 기준 upsert */
@Component
@RequiredArgsConstructor
public class AuthRepositoryAdapter implements AuthRepositoryPort {

    private final AuthJpaRepository authJpaRepository;
    private final AuthEntityMapper authEntityMapper;

    @Override
    public boolean existsByLoginId(String loginId) {
        return authJpaRepository.existsByLoginId(loginId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return authJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return authJpaRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsByIdentityKey(String identityKey) {
        return authJpaRepository.existsByIdentityKey(identityKey);
    }

    @Override
    public Optional<AuthDomain> findByLoginId(String loginId) {
        return authJpaRepository.findByLoginId(loginId)
                .map(authEntityMapper::toDomain);
    }

    @Override
    public Optional<AuthDomain> findByAuthUuid(String authUuid) {
        return authJpaRepository.findByAuthUuid(authUuid)
                .map(authEntityMapper::toDomain);
    }

    @Override
    public AuthDomain save(AuthDomain authDomain) {
        // 로그인 실패·잠금 갱신 등 기존 authUuid 행 update / 신규 insert
        AuthEntity entity = authJpaRepository.findByAuthUuid(authDomain.getAuthUuid())
                .map(existing -> {
                    authEntityMapper.updateEntity(existing, authDomain);
                    return existing;
                })
                .orElseGet(() -> authEntityMapper.toEntity(authDomain));
        AuthEntity saved = authJpaRepository.save(entity);
        return authEntityMapper.toDomain(saved);
    }
}
