package com.sparta.auth_service.application.service;

import com.sparta.auth_service.application.exception.AuthNotFoundException;
import com.sparta.auth_service.application.port.in.GetMemberJoinedAtUseCase;
import com.sparta.auth_service.application.port.in.dto.GetMemberJoinedAtResultDto;
import com.sparta.auth_service.application.port.out.AuthRepositoryPort;
import com.sparta.auth_service.domain.model.AuthDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMemberJoinedAtService implements GetMemberJoinedAtUseCase {

    private final AuthRepositoryPort authRepositoryPort;

    @Override
    public GetMemberJoinedAtResultDto getMemberJoinedAt(String memberUuid) {
        String normalizedMemberUuid = requireMemberUuid(memberUuid);
        AuthDomain auth = authRepositoryPort.findByAuthUuid(normalizedMemberUuid)
                .filter(AuthDomain::isActive)
                .orElseThrow(() -> new AuthNotFoundException("계정을 찾을 수 없습니다."));

        return GetMemberJoinedAtResultDto.builder()
                .memberUuid(auth.getAuthUuid())
                .joinedAt(auth.getCreatedAt())
                .build();
    }

    private String requireMemberUuid(String memberUuid) {
        if (memberUuid == null || memberUuid.isBlank()) {
            throw new IllegalArgumentException("memberUuid는 필수입니다.");
        }
        return memberUuid.trim();
    }
}
