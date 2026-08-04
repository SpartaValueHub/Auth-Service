package com.sparta.auth_service.domain.model;

import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.enums.VerificationStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 본인인증 이력 도메인 — requestToken·purpose·status·memberUuid(가입 후)만 보유.
 * CI·실명·전화는 PortOne 조회·응답 prefill 전용, DB에 PII 저장하지 않음.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdentityVerificationDomain {

    private Long identityVerificationId;
    /** 가입 완료 후 authUuid — null이면 sign-up 재사용 가능 */
    private String memberUuid;
    private VerificationPurpose purpose;
    /** PortOne 세션 토큰 — unique, 생성 후 변경 없음 */
    private String requestToken;
    private VerificationStatus status;
    private Instant createdAt;

    public static IdentityVerificationDomain createRequested(
            String requestToken,
            VerificationPurpose purpose
    ) {
        if (requestToken == null || requestToken.isBlank()) {
            throw new IllegalArgumentException("requestToken은 필수입니다.");
        }
        if (purpose == null) {
            throw new IllegalArgumentException("purpose는 필수입니다.");
        }
        return IdentityVerificationDomain.builder()
                .memberUuid(null)
                .purpose(purpose)
                .requestToken(requestToken.trim())
                .status(VerificationStatus.REQUESTED)
                .build();
    }

    public IdentityVerificationDomain markSuccess() {
        return copyWithStatus(VerificationStatus.SUCCESS, this.memberUuid);
    }

    public IdentityVerificationDomain withMemberUuid(String memberUuid) {
        if (memberUuid == null || memberUuid.isBlank()) {
            throw new IllegalArgumentException("memberUuid는 필수입니다.");
        }
        return IdentityVerificationDomain.builder()
                .identityVerificationId(this.identityVerificationId)
                .memberUuid(memberUuid.trim())
                .purpose(this.purpose)
                .requestToken(this.requestToken)
                .status(this.status)
                .createdAt(this.createdAt)
                .build();
    }

    public IdentityVerificationDomain markFailed() {
        return copyWithStatus(VerificationStatus.FAILED, this.memberUuid);
    }

    public IdentityVerificationDomain markCanceled() {
        return copyWithStatus(VerificationStatus.CANCELED, this.memberUuid);
    }

    public IdentityVerificationDomain markExpired() {
        return copyWithStatus(VerificationStatus.EXPIRED, this.memberUuid);
    }

    public IdentityVerificationDomain markRequested() {
        return copyWithStatus(VerificationStatus.REQUESTED, this.memberUuid);
    }

    private IdentityVerificationDomain copyWithStatus(VerificationStatus status, String memberUuid) {
        return IdentityVerificationDomain.builder()
                .identityVerificationId(this.identityVerificationId)
                .memberUuid(memberUuid)
                .purpose(this.purpose)
                .requestToken(this.requestToken)
                .status(status)
                .createdAt(this.createdAt)
                .build();
    }

    public static IdentityVerificationDomain reconstitute(
            Long identityVerificationId,
            String memberUuid,
            VerificationPurpose purpose,
            String requestToken,
            VerificationStatus status,
            Instant createdAt
    ) {
        return IdentityVerificationDomain.builder()
                .identityVerificationId(identityVerificationId)
                .memberUuid(memberUuid)
                .purpose(purpose)
                .requestToken(requestToken)
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    public boolean isSuccessful() {
        return status == VerificationStatus.SUCCESS;
    }

    public boolean isTerminal() {
        return status == VerificationStatus.SUCCESS
                || status == VerificationStatus.FAILED
                || status == VerificationStatus.CANCELED
                || status == VerificationStatus.EXPIRED;
    }

    /** sign-up에 재사용 가능: SUCCESS 이면서 아직 authUuid 미연결 */
    public boolean isAvailableForSignUp() {
        return isSuccessful() && (memberUuid == null || memberUuid.isBlank());
    }

    @Builder(access = AccessLevel.PRIVATE)
    private IdentityVerificationDomain(
            Long identityVerificationId,
            String memberUuid,
            VerificationPurpose purpose,
            String requestToken,
            VerificationStatus status,
            Instant createdAt
    ) {
        this.identityVerificationId = identityVerificationId;
        this.memberUuid = memberUuid;
        this.purpose = purpose;
        this.requestToken = requestToken;
        this.status = status;
        this.createdAt = createdAt;
    }
}
