package com.sparta.auth_service.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import com.sparta.auth_service.domain.enums.Gender;

/**
 * 인증 계정 도메인 — 로그인·CI·잠금 정책.
 * authUuid가 외부 식별자(PK 아님). 닉네임은 member-service 소유.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthDomain {

    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[a-z0-9]{4,20}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^01[016789]\\d{7,8}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()\\-+_=])[A-Za-z\\d!@#$%^&*()\\-+_=]{8,20}$"
    );

    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    /** 5회 연속 실패 시 계정 잠금 시간(분) */
    private static final long LOCK_MINUTES = 10;

    /** 외부 식별자 — JWT·API 응답에 사용, DB PK(auth_id)와 분리 */
    private String authUuid;
    private String loginId;
    private String memberName;
    private LocalDate birthdayDate;
    private String phoneNumber;
    private Gender gender;
    private String email;
    /** PortOne CI — 동일인 중복 가입 방지 */
    private String identityKey;
    private String passwordHash;
    private Instant passwordChangedAt;
    private int loginFailCount;
    private Instant lockedUntil;
    private Instant createdAt;
    private Instant updatedAt;

    public static AuthDomain createSignUp(
            String loginId,
            String passwordHash,
            String email,
            String memberName,
            LocalDate birthdayDate,
            String phoneNumber,
            Gender gender,
            String identityKey
    ) {
        validateLoginId(loginId);
        validatePasswordHash(passwordHash);
        String normalizedEmail = normalizeEmail(email);
        String trimmedMemberName = validateAndTrimMemberName(memberName);
        validateBirthdayDate(birthdayDate);
        validatePhoneNumber(phoneNumber);
        validateGender(gender);
        validateIdentityKey(identityKey);

        Instant now = Instant.now();
        return AuthDomain.builder()
                .authUuid(UUID.randomUUID().toString())
                .loginId(loginId.trim())
                .memberName(trimmedMemberName)
                .birthdayDate(birthdayDate)
                .phoneNumber(phoneNumber.trim())
                .gender(gender)
                .email(normalizedEmail)
                .identityKey(identityKey.trim())
                .passwordHash(passwordHash)
                .passwordChangedAt(now)
                .loginFailCount(0)
                .lockedUntil(null)
                .build();
    }

    public static AuthDomain reconstitute(
            String authUuid,
            String loginId,
            String memberName,
            LocalDate birthdayDate,
            String phoneNumber,
            Gender gender,
            String email,
            String identityKey,
            String passwordHash,
            Instant passwordChangedAt,
            int loginFailCount,
            Instant lockedUntil,
            Instant createdAt,
            Instant updatedAt
    ) {
        return AuthDomain.builder()
                .authUuid(authUuid)
                .loginId(loginId)
                .memberName(memberName)
                .birthdayDate(birthdayDate)
                .phoneNumber(phoneNumber)
                .gender(gender)
                .email(email)
                .identityKey(identityKey)
                .passwordHash(passwordHash)
                .passwordChangedAt(passwordChangedAt)
                .loginFailCount(loginFailCount)
                .lockedUntil(lockedUntil)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public AuthDomain changePasswordHash(String passwordHash) {
        validatePasswordHash(passwordHash);
        return AuthDomain.builder()
                .authUuid(this.authUuid)
                .loginId(this.loginId)
                .memberName(this.memberName)
                .birthdayDate(this.birthdayDate)
                .phoneNumber(this.phoneNumber)
                .gender(this.gender)
                .email(this.email)
                .identityKey(this.identityKey)
                .passwordHash(passwordHash)
                .passwordChangedAt(Instant.now())
                .loginFailCount(this.loginFailCount)
                .lockedUntil(this.lockedUntil)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public AuthDomain recordLoginFailure(Instant now) {
        int nextFailCount = this.loginFailCount + 1;
        // MAX_LOGIN_FAIL_COUNT 도달 시 lockedUntil 설정 — Domain에서 상태 전이만 담당
        Instant nextLockedUntil = nextFailCount >= MAX_LOGIN_FAIL_COUNT
                ? now.plusSeconds(LOCK_MINUTES * 60)
                : this.lockedUntil;

        return copyWithLoginState(nextFailCount, nextLockedUntil);
    }

    public AuthDomain resetLoginFailure() {
        return copyWithLoginState(0, null);
    }

    private AuthDomain copyWithLoginState(int loginFailCount, Instant lockedUntil) {
        return AuthDomain.builder()
                .authUuid(this.authUuid)
                .loginId(this.loginId)
                .memberName(this.memberName)
                .birthdayDate(this.birthdayDate)
                .phoneNumber(this.phoneNumber)
                .gender(this.gender)
                .email(this.email)
                .identityKey(this.identityKey)
                .passwordHash(this.passwordHash)
                .passwordChangedAt(this.passwordChangedAt)
                .loginFailCount(loginFailCount)
                .lockedUntil(lockedUntil)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    private static void validateLoginId(String loginId) {
        if (loginId == null || !LOGIN_ID_PATTERN.matcher(loginId.trim()).matches()) {
            throw new IllegalArgumentException("loginId는 영문 소문자와 숫자 조합 4~20자여야 합니다.");
        }
    }

    private static void validatePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash는 필수입니다.");
        }
    }

    /** 평문 비밀번호 검증 — Application에서 encode 전에 호출 */
    public static void validatePlainPassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "비밀번호는 8~20자, 영문 대문자·소문자·숫자·특수문자(!@#$%^&*()-+_=)를 각각 1자 이상 포함해야 합니다."
            );
        }
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email은 50자 이하의 유효한 형식이어야 합니다.");
        }
        String trimmed = email.trim();
        if (trimmed.length() > 50) {
            throw new IllegalArgumentException("email은 50자 이하의 유효한 형식이어야 합니다.");
        }
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("email 형식이 올바르지 않습니다.");
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static String validateAndTrimMemberName(String memberName) {
        if (memberName == null || memberName.isBlank()) {
            throw new IllegalArgumentException("memberName은 필수입니다.");
        }
        String trimmed = memberName.trim();
        if (trimmed.length() > 30) {
            throw new IllegalArgumentException("memberName은 30자 이하여야 합니다.");
        }
        return trimmed;
    }

    private static void validateBirthdayDate(LocalDate birthdayDate) {
        if (birthdayDate == null) {
            throw new IllegalArgumentException("birthdayDate는 필수입니다.");
        }
        if (birthdayDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("birthdayDate는 미래 날짜일 수 없습니다.");
        }
    }

    private static void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || !PHONE_PATTERN.matcher(phoneNumber.trim()).matches()) {
            throw new IllegalArgumentException("phoneNumber 형식이 올바르지 않습니다.");
        }
    }

    private static void validateGender(Gender gender) {
        if (gender == null) {
            throw new IllegalArgumentException("gender는 필수입니다.");
        }
    }

    private static void validateIdentityKey(String identityKey) {
        // CI(identityKey) — 동일인 중복 가입 방지용, auth 테이블에만 저장
        if (identityKey == null || identityKey.isBlank()) {
            throw new IllegalArgumentException("identityKey(CI)는 필수입니다.");
        }
    }

    @Builder(access = AccessLevel.PRIVATE)
    private AuthDomain(
            String authUuid,
            String loginId,
            String memberName,
            LocalDate birthdayDate,
            String phoneNumber,
            Gender gender,
            String email,
            String identityKey,
            String passwordHash,
            Instant passwordChangedAt,
            int loginFailCount,
            Instant lockedUntil,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.authUuid = authUuid;
        this.loginId = loginId;
        this.memberName = memberName;
        this.birthdayDate = birthdayDate;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.email = email;
        this.identityKey = identityKey;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = passwordChangedAt;
        this.loginFailCount = loginFailCount;
        this.lockedUntil = lockedUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
