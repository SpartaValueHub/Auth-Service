package com.unionclass.auth_service.application.service;

import com.unionclass.auth_service.application.port.in.AuthUseCase;
import com.unionclass.auth_service.application.port.in.dto.AuthSignUpRequestDto;
import com.unionclass.auth_service.application.port.in.dto.AuthSignUpResultDto;
import com.unionclass.auth_service.application.port.out.AuthRepositoryPort;
import com.unionclass.auth_service.application.port.out.PasswordEncoderPort;
import com.unionclass.auth_service.domain.model.AuthDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService implements AuthUseCase {

    private final AuthRepositoryPort authRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    @Override
    @Transactional
    public AuthSignUpResultDto signUp(AuthSignUpRequestDto requestDto) {
        AuthDomain authDomain = AuthDomain.createSignUp(
                requestDto.getLogInId(),
                requestDto.getPassword(),
                requestDto.getEmail(),
                requestDto.getName(),
                requestDto.getPhone()
        );

        validateDuplication(authDomain);

        AuthDomain encodedAuth = authDomain.withEncodedPassword(
                passwordEncoderPort.encode(authDomain.getPassword())
        );
        AuthDomain saved = authRepositoryPort.save(encodedAuth);

        return AuthSignUpResultDto.builder()
                .userId(saved.getUserId())
                .logInId(saved.getLogInId())
                .email(saved.getEmail())
                .name(saved.getName())
                .build();
    }

    private void validateDuplication(AuthDomain authDomain) {
        if (authRepositoryPort.existsByLogInId(authDomain.getLogInId())) {
            throw new IllegalArgumentException("이미 사용 중인 loginId입니다.");
        }
        if (authRepositoryPort.existsByEmailAndNotDeleted(authDomain.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 email입니다.");
        }
        if (authRepositoryPort.existsByPhoneAndNotDeleted(authDomain.getPhone())) {
            throw new IllegalArgumentException("이미 사용 중인 phone입니다.");
        }
    }
}
