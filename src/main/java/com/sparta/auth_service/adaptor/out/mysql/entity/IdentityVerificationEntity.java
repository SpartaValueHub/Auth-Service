package com.sparta.auth_service.adaptor.out.mysql.entity;

import com.sparta.auth_service.domain.enums.VerificationMethod;
import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/** 본인인증 이력 — 원본 CI는 저장하지 않고 HMAC-SHA256 처리한 ci_hash만 저장 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "identity_verifications")
public class IdentityVerificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identity_verification_id")
    private Long identityVerificationId;

    @Column(name = "verification_uuid", nullable = false, unique = true, length = 36)
    private String verificationUuid;

    /** 인증 사용 완료 후 memberUuid 연결 — null이면 아직 회원과 연결되지 않은 인증 */
    @Column(name = "member_uuid", length = 36)
    private String memberUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private VerificationPurpose purpose;

    @Column(name = "request_token", nullable = false, unique = true, length = 255)
    private String requestToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", length = 30)
    private VerificationMethod verificationMethod;

    @Column(name = "ci_hash", length = 64)
    private String ciHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    private IdentityVerificationEntity(
            String verificationUuid,
            String memberUuid,
            VerificationPurpose purpose,
            String requestToken,
            VerificationMethod verificationMethod,
            String ciHash,
            VerificationStatus verificationStatus,
            Instant verifiedAt
    ) {
        this.verificationUuid = verificationUuid;
        this.memberUuid = memberUuid;
        this.purpose = purpose;
        this.requestToken = requestToken;
        this.verificationMethod = verificationMethod;
        this.ciHash = ciHash;
        this.verificationStatus = verificationStatus;
        this.verifiedAt = verifiedAt;
    }

    /** requestToken·verificationUuid는 생성 후 변경 없음 */
    public void updateVerification(
            String memberUuid,
            VerificationPurpose purpose,
            VerificationMethod verificationMethod,
            String ciHash,
            VerificationStatus verificationStatus,
            Instant verifiedAt
    ) {
        this.memberUuid = memberUuid;
        this.purpose = purpose;
        this.verificationMethod = verificationMethod;
        this.ciHash = ciHash;
        this.verificationStatus = verificationStatus;
        this.verifiedAt = verifiedAt;
    }
}
