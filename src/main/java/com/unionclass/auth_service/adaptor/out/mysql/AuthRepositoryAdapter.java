package com.unionclass.auth_service.adaptor.out.mysql;

import com.unionclass.auth_service.adaptor.out.mysql.entity.AuthEntity;
import com.unionclass.auth_service.adaptor.out.mysql.mapper.AuthEntityMapper;
import com.unionclass.auth_service.adaptor.out.mysql.repository.AuthRepository;
import com.unionclass.auth_service.application.port.out.AuthRepositoryPort;
import com.unionclass.auth_service.domain.model.AuthDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthRepositoryAdapter implements AuthRepositoryPort {

    private final AuthRepository authRepository;
    private final AuthEntityMapper authEntityMapper;

    @Override
    public boolean existsByLogInId(String logInId) {
        return authRepository.existsByLogInId(logInId);
    }

    @Override
    public boolean existsByEmailAndNotDeleted(String email) {
        return authRepository.existsByEmailAndDeleted(email, false);
    }

    @Override
    public boolean existsByPhoneAndNotDeleted(String phone) {
        return authRepository.existsByPhoneAndDeleted(phone, false);
    }

    @Override
    public Optional<AuthDomain> findByLogInIdAndNotDeleted(String logInId) {
        return authRepository.findByLogInIdAndDeleted(logInId, false)
                .map(authEntityMapper::toDomain);
    }

    @Override
    public AuthDomain save(AuthDomain authDomain) {
        AuthEntity entity = authEntityMapper.toEntity(authDomain);
        AuthEntity saved = authRepository.save(entity);
        return authEntityMapper.toDomain(saved);
    }
}
