package com.sparta.auth_service.application.service;

import com.sparta.auth_service.application.exception.DuplicateResourceException;
import com.sparta.auth_service.application.exception.IdentityVerificationAlreadyUsedException;
import com.sparta.auth_service.application.exception.IdentityVerificationFailedException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotFoundException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotReadyException;
import com.sparta.auth_service.application.port.out.AuthRepositoryPort;
import com.sparta.auth_service.application.port.out.IdentityVerificationRepositoryPort;
import com.sparta.auth_service.application.port.out.SignupIdentityClaimPort;
import com.sparta.auth_service.application.port.out.SignupCompletionTokenPort;
import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.model.AuthDomain;
import com.sparta.auth_service.domain.model.IdentityVerificationDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원가입의 DB 변경만 짧은 단일 트랜잭션으로 처리한다. */
@Service
@RequiredArgsConstructor
public class SignupPersistenceService {

    private final AuthRepositoryPort authRepositoryPort;
    private final IdentityVerificationRepositoryPort identityVerificationRepositoryPort;
    private final SignupIdentityClaimPort signupIdentityClaimPort;
    private final SignupCompletionTokenPort signupCompletionTokenPort;

    @Transactional
    public AuthDomain persist(
            String requestToken,
            String expectedCiHash,
            AuthDomain authDomain,
            String completionTokenId,
            long completionTokenTtlSeconds
    ) {
        IdentityVerificationDomain verification = identityVerificationRepositoryPort.findByRequestToken(requestToken)
                .orElseThrow(() -> new IdentityVerificationNotFoundException("본인인증 이력을 찾을 수 없습니다."));

        if (verification.getPurpose() != VerificationPurpose.SIGN_UP) {
            throw new IllegalArgumentException("SIGN_UP 본인인증이 필요합니다.");
        }
        if (!verification.isAvailableForSignUp()) {
            if (verification.isSuccessful()) {
                throw new IdentityVerificationAlreadyUsedException("이미 사용된 본인인증 요청입니다.");
            }
            throw new IdentityVerificationNotReadyException("본인인증이 완료되지 않았습니다.");
        }
        if (!expectedCiHash.equals(verification.getCiHash())) {
            throw new IdentityVerificationFailedException("본인인증 정보가 일치하지 않습니다.");
        }

        validateDuplication(authDomain, expectedCiHash);
        signupIdentityClaimPort.claim(expectedCiHash, authDomain.getAuthUuid());
        AuthDomain saved = authRepositoryPort.save(authDomain);
        identityVerificationRepositoryPort.save(verification.withMemberUuid(saved.getAuthUuid()));
        // Redis 저장 실패 시 예외를 전파해 auth/CI claim/본인인증 연결을 함께 롤백한다.
        signupCompletionTokenPort.save(
                saved.getAuthUuid(),
                completionTokenId,
                completionTokenTtlSeconds
        );
        return saved;
    }

    private void validateDuplication(AuthDomain authDomain, String ciHash) {
        if (authRepositoryPort.existsByLoginId(authDomain.getLoginId())) {
            throw new DuplicateResourceException("AUTH_DUPLICATE_LOGIN_ID", "이미 사용 중인 loginId입니다.");
        }
        if (authRepositoryPort.existsByEmail(authDomain.getEmail())) {
            throw new DuplicateResourceException("AUTH_DUPLICATE_EMAIL", "이미 사용 중인 email입니다.");
        }
        if (authRepositoryPort.existsByPhoneNumber(authDomain.getPhoneNumber())) {
            throw new DuplicateResourceException("AUTH_DUPLICATE_PHONE", "이미 사용 중인 phoneNumber입니다.");
        }
        // claim UNIQUE 인덱스 조회 — 탈퇴 후 release된 CI는 재가입 가능
        if (signupIdentityClaimPort.existsByCiHash(ciHash)) {
            throw new DuplicateResourceException("AUTH_DUPLICATE_IDENTITY", "이미 가입된 본인인증 정보입니다.");
        }
    }
}
