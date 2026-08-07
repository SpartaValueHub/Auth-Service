package com.sparta.auth_service.adaptor.out.mysql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
        name = "signup_identity_claims",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_signup_identity_claim_ci_hash", columnNames = "ci_hash"),
                @UniqueConstraint(name = "uk_signup_identity_claim_auth_uuid", columnNames = "auth_uuid")
        }
)
public class SignupIdentityClaimEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "signup_identity_claim_id")
    private Long signupIdentityClaimId;

    @Column(name = "ci_hash", nullable = false, length = 64)
    private String ciHash;

    @Column(name = "auth_uuid", nullable = false, length = 36)
    private String authUuid;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static SignupIdentityClaimEntity create(String ciHash, String authUuid) {
        if (ciHash == null || ciHash.isBlank()) {
            throw new IllegalArgumentException("ciHash는 필수입니다.");
        }
        if (authUuid == null || authUuid.isBlank()) {
            throw new IllegalArgumentException("authUuid는 필수입니다.");
        }
        SignupIdentityClaimEntity entity = new SignupIdentityClaimEntity();
        entity.ciHash = ciHash.trim();
        entity.authUuid = authUuid.trim();
        return entity;
    }
}
