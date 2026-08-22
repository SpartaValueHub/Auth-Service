package com.sparta.auth_service.domain.model;

import com.sparta.auth_service.domain.enums.VerificationMethod;
import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.enums.VerificationStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * 본인인증 이력 도메인 — CI는 ciHash(HMAC)만 저장, 평문·암호문은 보관하지 않음.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdentityVerificationDomain {

    private Long identityVerificationId;
    private String verificationUuid;
    /** 인증 사용 완료 후 memberUuid 연결 — null이면 아직 회원과 연결되지 않은 인증 */
    private String memberUuid;
    private VerificationPurpose purpose;
    /** PortOne 세션 토큰 — unique, 생성 후 변경 없음 */
    private String requestToken;
    private VerificationMethod verificationMethod;
    /** HMAC-SHA256 CI 검색 해시 */
    private String ciHash;
    private VerificationStatus verificationStatus;
    private Instant verifiedAt;
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
                .verificationUuid(UUID.randomUUID().toString())
                .memberUuid(null)
                .purpose(purpose)
                .requestToken(requestToken.trim())
                .verificationStatus(VerificationStatus.REQUESTED)
                .build();
    }

    public IdentityVerificationDomain markVerified(
            VerificationMethod verificationMethod,
            String ciHash,
            Instant verifiedAt
    ) {
        if (verificationStatus != VerificationStatus.REQUESTED) {
            throw new IllegalStateException("요청 상태의 인증만 성공 처리할 수 있습니다.");
        }
        if (verificationMethod == null) {
            throw new IllegalArgumentException("verificationMethod는 필수입니다.");
        }
        if (ciHash == null || ciHash.isBlank()) {
            throw new IllegalArgumentException("ciHash는 필수입니다.");
        }
        if (verifiedAt == null) {
            throw new IllegalArgumentException("verifiedAt은 필수입니다.");
        }
        return IdentityVerificationDomain.builder()
                .identityVerificationId(this.identityVerificationId)
                .verificationUuid(this.verificationUuid)
                .memberUuid(this.memberUuid)
                .purpose(this.purpose)
                .requestToken(this.requestToken)
                .verificationMethod(verificationMethod)
                .ciHash(ciHash)
                .verificationStatus(VerificationStatus.SUCCESS)
                .verifiedAt(verifiedAt)
                .createdAt(this.createdAt)
                .build();
    }

    public IdentityVerificationDomain withMemberUuid(String memberUuid) {
        if (memberUuid == null || memberUuid.isBlank()) {
            throw new IllegalArgumentException("memberUuid는 필수입니다.");
        }
        if (verificationStatus != VerificationStatus.SUCCESS) {
            throw new IllegalStateException("성공한 인증만 회원과 연결할 수 있습니다.");
        }
        if (this.memberUuid != null && !this.memberUuid.isBlank()) {
            throw new IllegalStateException("이미 회원과 연결된 인증입니다.");
        }
        return IdentityVerificationDomain.builder()
                .identityVerificationId(this.identityVerificationId)
                .verificationUuid(this.verificationUuid)
                .memberUuid(memberUuid.trim())
                .purpose(this.purpose)
                .requestToken(this.requestToken)
                .verificationMethod(this.verificationMethod)
                .ciHash(this.ciHash)
                .verificationStatus(this.verificationStatus)
                .verifiedAt(this.verifiedAt)
                .createdAt(this.createdAt)
                .build();
    }

    public IdentityVerificationDomain markFailed() {
        if (verificationStatus != VerificationStatus.REQUESTED) {
            throw new IllegalStateException("요청 상태의 인증만 실패 처리할 수 있습니다.");
        }
        return IdentityVerificationDomain.builder()
                .identityVerificationId(this.identityVerificationId)
                .verificationUuid(this.verificationUuid)
                .memberUuid(this.memberUuid)
                .purpose(this.purpose)
                .requestToken(this.requestToken)
                .verificationMethod(null)
                .ciHash(null)
                .verificationStatus(VerificationStatus.FAILED)
                .verifiedAt(null)
                .createdAt(this.createdAt)
                .build();
    }

    /**
     * DB에서 읽은 값을 그대로 복원한다. 상태 전이·검증·값 변경 없음.
     */
    public static IdentityVerificationDomain reconstitute(
            Long identityVerificationId,
            String verificationUuid,
            String memberUuid,
            VerificationPurpose purpose,
            String requestToken,
            VerificationMethod verificationMethod,
            String ciHash,
            VerificationStatus verificationStatus,
            Instant verifiedAt,
            Instant createdAt
    ) {
        return IdentityVerificationDomain.builder()
                .identityVerificationId(identityVerificationId)
                .verificationUuid(verificationUuid)
                .memberUuid(memberUuid)
                .purpose(purpose)
                .requestToken(requestToken)
                .verificationMethod(verificationMethod)
                .ciHash(ciHash)
                .verificationStatus(verificationStatus)
                .verifiedAt(verifiedAt)
                .createdAt(createdAt)
                .build();
    }

    public boolean isSuccessful() {
        return verificationStatus == VerificationStatus.SUCCESS;
    }

    /** sign-up에 재사용 가능: SIGN_UP purpose, SUCCESS, memberUuid 미연결, ciHash 저장됨 */
    public boolean isAvailableForSignUp() {
        return purpose == VerificationPurpose.SIGN_UP
                && isSuccessful()
                && (memberUuid == null || memberUuid.isBlank())
                && ciHash != null
                && !ciHash.isBlank();
    }

    /** 탈퇴 본인인증: WITHDRAWAL purpose, SUCCESS, ciHash 저장됨 (memberUuid 연결 여부는 Application에서 처리) */
    public boolean isAvailableForWithdrawal() {
        return purpose == VerificationPurpose.WITHDRAWAL
                && isSuccessful()
                && ciHash != null
                && !ciHash.isBlank();
    }

    public boolean isLinkedToMember(String memberUuid) {
        if (memberUuid == null || memberUuid.isBlank()) {
            return false;
        }
        return this.memberUuid != null && memberUuid.trim().equals(this.memberUuid);
    }

    public boolean hasLinkedMember() {
        return memberUuid != null && !memberUuid.isBlank();
    }

    @Builder(access = AccessLevel.PRIVATE)
    private IdentityVerificationDomain(
            Long identityVerificationId,
            String verificationUuid,
            String memberUuid,
            VerificationPurpose purpose,
            String requestToken,
            VerificationMethod verificationMethod,
            String ciHash,
            VerificationStatus verificationStatus,
            Instant verifiedAt,
            Instant createdAt
    ) {
        this.identityVerificationId = identityVerificationId;
        this.verificationUuid = verificationUuid;
        this.memberUuid = memberUuid;
        this.purpose = purpose;
        this.requestToken = requestToken;
        this.verificationMethod = verificationMethod;
        this.ciHash = ciHash;
        this.verificationStatus = verificationStatus;
        this.verifiedAt = verifiedAt;
        this.createdAt = createdAt;
    }
}
