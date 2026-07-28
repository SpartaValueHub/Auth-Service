package com.unionclass.auth_service.application.service;

import com.unionclass.auth_service.application.exception.UnauthorizedException;
import com.unionclass.auth_service.application.port.in.AuthUseCase;
import com.unionclass.auth_service.application.port.in.dto.AuthSignInRequestDto;
import com.unionclass.auth_service.application.port.in.dto.AuthSignInResultDto;
import com.unionclass.auth_service.application.port.in.dto.AuthSignUpRequestDto;
import com.unionclass.auth_service.application.port.in.dto.AuthSignUpResultDto;
import com.unionclass.auth_service.application.port.out.AuthRepositoryPort;
import com.unionclass.auth_service.application.port.out.PasswordEncoderPort;
import com.unionclass.auth_service.application.port.out.TokenProviderPort;
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
    private final TokenProviderPort tokenProviderPort;

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

    @Override
    public AuthSignInResultDto signIn(AuthSignInRequestDto requestDto) {
        String logInId = requestDto.getLogInId() == null ? "" : requestDto.getLogInId().trim();
        String password = requestDto.getPassword();

        if (logInId.isBlank() || password == null || password.isBlank()) {
            throw new UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        AuthDomain auth = authRepositoryPort.findByLogInIdAndNotDeleted(logInId)
                .orElseThrow(() -> new UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoderPort.matches(password, auth.getPassword())) {
            throw new UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return AuthSignInResultDto.builder()
                .accessToken(tokenProviderPort.createAccessToken(
                        auth.getUserId(),
                        auth.getLogInId(),
                        auth.getName()
                ))
                .refreshToken(tokenProviderPort.createRefreshToken(auth.getUserId()))
                .userId(auth.getUserId())
                .logInId(auth.getLogInId())
                .name(auth.getName())
                .email(auth.getEmail())
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
