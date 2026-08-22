package com.sparta.auth_service.application.service;

import com.sparta.auth_service.application.exception.AuthIdentityMismatchException;
import com.sparta.auth_service.application.exception.AuthNotFoundException;
import com.sparta.auth_service.application.exception.IdentityVerificationAlreadyUsedException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotFoundException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotReadyException;
import com.sparta.auth_service.application.exception.MemberNotActiveException;
import com.sparta.auth_service.application.port.in.WithdrawMemberUseCase;
import com.sparta.auth_service.application.port.in.dto.WithdrawMemberRequestDto;
import com.sparta.auth_service.application.port.out.AuthRepositoryPort;
import com.sparta.auth_service.application.port.out.IdentityVerificationRepositoryPort;
import com.sparta.auth_service.application.port.out.SessionInvalidationPort;
import com.sparta.auth_service.application.port.out.SignupIdentityClaimPort;
import com.sparta.auth_service.domain.model.AuthDomain;
import com.sparta.auth_service.domain.model.IdentityVerificationDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
public class WithdrawMemberService implements WithdrawMemberUseCase {

    private final AuthRepositoryPort authRepositoryPort;
    private final IdentityVerificationRepositoryPort identityVerificationRepositoryPort;
    private final SignupIdentityClaimPort signupIdentityClaimPort;
    private final SessionInvalidationPort sessionInvalidationPort;

    @Override
    @Transactional
    public void withdraw(WithdrawMemberRequestDto requestDto) {
        String authUuid = requireAuthUuid(requestDto.getAuthUuid());
        String requestToken = requireRequestToken(requestDto.getRequestToken());

        AuthDomain auth = authRepositoryPort.findByAuthUuid(authUuid)
                .orElseThrow(() -> new AuthNotFoundException("계정을 찾을 수 없습니다."));

        IdentityVerificationDomain withdrawalVerification = identityVerificationRepositoryPort
                .findByRequestToken(requestToken)
                .orElseThrow(() -> new IdentityVerificationNotFoundException("본인인증 내역을 찾을 수 없습니다."));

        if (!withdrawalVerification.isAvailableForWithdrawal()) {
            throw new IdentityVerificationNotReadyException("탈퇴용 본인인증이 완료되지 않았습니다.");
        }
        if (withdrawalVerification.hasLinkedMember() && !withdrawalVerification.isLinkedToMember(authUuid)) {
            throw new IdentityVerificationAlreadyUsedException("이미 사용된 본인인증입니다.");
        }

        IdentityVerificationDomain signUpVerification = identityVerificationRepositoryPort
                .findSignUpLinkedByMemberUuid(authUuid)
                .orElseThrow(() -> new IdentityVerificationNotFoundException("가입 본인인증 이력을 찾을 수 없습니다."));

        if (!matchesCiHash(withdrawalVerification.getCiHash(), signUpVerification.getCiHash())) {
            throw new AuthIdentityMismatchException("본인인증 정보가 계정과 일치하지 않습니다.");
        }

        if (!auth.isActive() && !auth.isWithdrawn()) {
            throw new MemberNotActiveException("현재 탈퇴할 수 없는 계정입니다.");
        }

        // anonymize 미반영 탈퇴 행도 재호출 시 UNIQUE 해제
        AuthDomain withdrawn = auth.withdraw();
        if (withdrawn != auth) {
            authRepositoryPort.save(withdrawn);
        }

        // CI 재가입 허용 — UNIQUE claim 해제 (없으면 no-op)
        signupIdentityClaimPort.releaseByAuthUuid(authUuid);

        if (!withdrawalVerification.hasLinkedMember()) {
            identityVerificationRepositoryPort.save(withdrawalVerification.withMemberUuid(authUuid));
        }

        sessionInvalidationPort.revokeAllSessions(authUuid);
    }

    private static boolean matchesCiHash(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(leftBytes, rightBytes);
    }

    private static String requireAuthUuid(String authUuid) {
        if (authUuid == null || authUuid.isBlank()) {
            throw new IllegalArgumentException("authUuid는 필수입니다.");
        }
        return authUuid.trim();
    }

    private static String requireRequestToken(String requestToken) {
        if (requestToken == null || requestToken.isBlank()) {
            throw new IllegalArgumentException("requestToken는 필수입니다.");
        }
        return requestToken.trim();
    }
}
