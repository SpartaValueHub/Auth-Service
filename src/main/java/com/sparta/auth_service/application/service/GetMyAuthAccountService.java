package com.sparta.auth_service.application.service;

import com.sparta.auth_service.application.exception.AuthNotFoundException;
import com.sparta.auth_service.application.port.in.GetMyAuthAccountUseCase;
import com.sparta.auth_service.application.port.in.dto.GetMyAuthAccountResultDto;
import com.sparta.auth_service.application.port.out.AuthRepositoryPort;
import com.sparta.auth_service.domain.model.AuthDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMyAuthAccountService implements GetMyAuthAccountUseCase {

    private final AuthRepositoryPort authRepositoryPort;

    @Override
    public GetMyAuthAccountResultDto getMyAuthAccount(String authUuid) {
        String normalizedAuthUuid = requireAuthUuid(authUuid);
        AuthDomain auth = authRepositoryPort.findByAuthUuid(normalizedAuthUuid)
                .orElseThrow(() -> new AuthNotFoundException("계정을 찾을 수 없습니다."));

        return GetMyAuthAccountResultDto.builder()
                .authUuid(auth.getAuthUuid())
                .loginId(auth.getLoginId())
                .email(auth.getEmail())
                .phoneNumber(auth.getPhoneNumber())
                .joinedAt(auth.getCreatedAt())
                .build();
    }

    private static String requireAuthUuid(String authUuid) {
        if (authUuid == null || authUuid.isBlank()) {
            throw new IllegalArgumentException("authUuid는 필수입니다.");
        }
        return authUuid.trim();
    }
}
