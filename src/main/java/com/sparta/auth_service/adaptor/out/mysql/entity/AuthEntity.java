package com.sparta.auth_service.adaptor.out.mysql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;

/** auth 테이블 — auth_id(PK) 내부용, 외부 식별자는 auth_uuid */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "auth")
public class AuthEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_id")
    private Long authId;

    @Column(name = "auth_uuid", nullable = false, unique = true, length = 36)
    private String authUuid;

    @Column(name = "login_id", nullable = false, unique = true, length = 20)
    private String loginId;

    @Column(name = "member_name", nullable = false, length = 50)
    private String memberName;

    @Column(name = "birthday_date", nullable = false)
    private LocalDate birthdayDate;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /** PortOne CI — 동일인 중복 가입 방지, unique */
    @Column(name = "identity_key", nullable = false, unique = true, length = 100)
    private String identityKey;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    /** AuthDomain.recordLoginFailure와 동기 — 5회 도달 시 lockedUntil 설정 */
    @Column(name = "login_fail_count", nullable = false)
    private int loginFailCount;

    /** null이면 미잠금; Domain.isLocked(now)와 동일 의미 */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    private AuthEntity(
            String authUuid,
            String loginId,
            String memberName,
            LocalDate birthdayDate,
            String phoneNumber,
            String email,
            String identityKey,
            String passwordHash,
            Instant passwordChangedAt,
            int loginFailCount,
            Instant lockedUntil
    ) {
        this.authUuid = authUuid;
        this.loginId = loginId;
        this.memberName = memberName;
        this.birthdayDate = birthdayDate;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.identityKey = identityKey;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = passwordChangedAt;
        this.loginFailCount = loginFailCount;
        this.lockedUntil = lockedUntil;
    }
}
