package com.sparta.auth_service.adaptor.out.mysql.entity;

import com.sparta.auth_service.domain.enums.Gender;
import com.sparta.auth_service.domain.enums.MemberStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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

/** auth 테이블 — auth_id(PK) 내부용, 외부 식별자는 auth_uuid. CI는 identity_verifications에만 저장 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
        name = "auth",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_auth_auth_uuid", columnNames = "auth_uuid"),
                @UniqueConstraint(name = "uk_auth_login_id", columnNames = "login_id"),
                @UniqueConstraint(name = "uk_auth_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_auth_phone_number", columnNames = "phone_number")
        }
)
public class AuthEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_id")
    private Long authId;

    @Column(name = "auth_uuid", nullable = false, length = 36)
    private String authUuid;

    @Column(name = "login_id", nullable = false, length = 20)
    private String loginId;

    @Column(name = "member_name", nullable = false, length = 50)
    private String memberName;

    @Column(name = "birthday_date", nullable = false)
    private LocalDate birthdayDate;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_status", nullable = false, length = 20)
    private MemberStatus memberStatus;

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
            Gender gender,
            String email,
            String passwordHash,
            Instant passwordChangedAt,
            MemberStatus memberStatus
    ) {
        this.authUuid = authUuid;
        this.loginId = loginId;
        this.memberName = memberName;
        this.birthdayDate = birthdayDate;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = passwordChangedAt;
        this.memberStatus = memberStatus;
    }
}
