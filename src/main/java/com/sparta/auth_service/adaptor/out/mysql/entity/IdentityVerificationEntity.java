package com.sparta.auth_service.adaptor.out.mysql.entity;

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
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/** 본인인증 이력 — PII 컬럼 없음, status·purpose·requestToken·member_uuid(가입 후)만 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "identity_verifications")
public class IdentityVerificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identity_verification_id")
    private Long identityVerificationId;

    /** 가입 완료 후 authUuid 연결 — null이면 sign-up 재사용 가능(SUCCESS 시) */
    @Column(name = "member_uuid", length = 36)
    private String memberUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private VerificationPurpose purpose;

    @Column(name = "request_token", nullable = false, unique = true, length = 255)
    private String requestToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VerificationStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    private IdentityVerificationEntity(
            String memberUuid,
            VerificationPurpose purpose,
            String requestToken,
            VerificationStatus status
    ) {
        this.memberUuid = memberUuid;
        this.purpose = purpose;
        this.requestToken = requestToken;
        this.status = status;
    }
}
