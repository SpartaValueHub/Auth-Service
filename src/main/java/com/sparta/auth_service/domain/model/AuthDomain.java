package com.sparta.auth_service.domain.model;

import com.sparta.auth_service.domain.enums.Gender;
import com.sparta.auth_service.domain.enums.MemberStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 인증 계정 도메인 — 로그인·회원 상태 정책.
 * authUuid가 외부 식별자(PK 아님). CI는 identity_verifications 이력에만 저장.
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
    // 탈퇴 식별자 prefix — authUuid(compact)에서 유도해 UNIQUE 충돌·재사용을 막음
    private static final String WITHDRAWN_IDENTIFIER_PREFIX = "w";
    // 탈퇴 email 도메인 — 실사용 불가 local-part만 보관
    private static final String WITHDRAWN_EMAIL_DOMAIN = "@w.invalid";
    // loginId·phone_number 컬럼 길이(20)에 맞춘 compact uuid 사용 길이
    private static final int WITHDRAWN_COMPACT_ID_LENGTH = 19;

    /** 외부 식별자 — JWT·API 응답에 사용, DB PK(auth_id)와 분리 */
    private String authUuid;
    private String loginId;
    private String memberName;
    private LocalDate birthdayDate;
    private String phoneNumber;
    private Gender gender;
    private String email;
    private String passwordHash;
    private Instant passwordChangedAt;
    private MemberStatus memberStatus;
    private Instant createdAt;
    private Instant updatedAt;

    public static AuthDomain createSignUp(
            String loginId,
            String passwordHash,
            String email,
            String memberName,
            LocalDate birthdayDate,
            String phoneNumber,
            Gender gender
    ) {
        validateLoginId(loginId);
        validatePasswordHash(passwordHash);
        String normalizedEmail = normalizeEmail(email);
        String trimmedMemberName = validateAndTrimMemberName(memberName);
        validateBirthdayDate(birthdayDate);
        validatePhoneNumber(phoneNumber);
        validateGender(gender);

        Instant now = Instant.now();
        return AuthDomain.builder()
                .authUuid(UUID.randomUUID().toString())
                .loginId(normalizeLoginIdForLookup(loginId))
                .memberName(trimmedMemberName)
                .birthdayDate(birthdayDate)
                .phoneNumber(normalizePhoneNumberForLookup(phoneNumber))
                .gender(gender)
                .email(normalizedEmail)
                .passwordHash(passwordHash)
                .passwordChangedAt(now)
                .memberStatus(MemberStatus.ACTIVE)
                .build();
    }

    /** existsBy·availability 조회용 — createSignUp 저장값과 동일한 loginId 정규화(trim) */
    public static String normalizeLoginIdForLookup(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId는 필수입니다.");
        }
        return loginId.trim();
    }

    /** existsBy·availability 조회용 — createSignUp 저장값과 동일한 email 정규화(trim + Locale.ROOT lowercase) */
    public static String normalizeEmailForLookup(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email은 필수입니다.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** existsBy·availability 조회용 — createSignUp 저장값과 동일한 phoneNumber 정규화(trim) */
    public static String normalizePhoneNumberForLookup(String phoneNumber) {
        if (phoneNumber == null) {
            throw new IllegalArgumentException("phoneNumber는 필수입니다.");
        }
        return phoneNumber.trim();
    }

    public static AuthDomain reconstitute(
            String authUuid,
            String loginId,
            String memberName,
            LocalDate birthdayDate,
            String phoneNumber,
            Gender gender,
            String email,
            String passwordHash,
            Instant passwordChangedAt,
            MemberStatus memberStatus,
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
                .passwordHash(passwordHash)
                .passwordChangedAt(passwordChangedAt)
                .memberStatus(memberStatus)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public boolean isActive() {
        return memberStatus == MemberStatus.ACTIVE;
    }

    public boolean isWithdrawn() {
        return memberStatus == MemberStatus.WITHDRAWN;
    }

    private static String withdrawnLoginId(String authUuid) {
        return WITHDRAWN_IDENTIFIER_PREFIX + compactUuid(authUuid).substring(0, WITHDRAWN_COMPACT_ID_LENGTH);
    }

    private static String withdrawnEmail(String authUuid) {
        return WITHDRAWN_IDENTIFIER_PREFIX + compactUuid(authUuid) + WITHDRAWN_EMAIL_DOMAIN;
    }

    private static String withdrawnPhoneNumber(String authUuid) {
        return WITHDRAWN_IDENTIFIER_PREFIX + compactUuid(authUuid).substring(0, WITHDRAWN_COMPACT_ID_LENGTH);
    }

    /** 회원 탈퇴 — ACTIVE→WITHDRAWN + 식별자 anonymize. 이미 완료면 동일 인스턴스(멱등) */
    public AuthDomain withdraw() {
        if (memberStatus != MemberStatus.ACTIVE && memberStatus != MemberStatus.WITHDRAWN) {
            throw new IllegalStateException("탈퇴할 수 없는 계정 상태입니다.");
        }
        // 탈퇴 식별자 — authUuid 기반이라 계정마다 충돌 없이 UNIQUE 해제
        String nextLoginId = withdrawnLoginId(authUuid);
        String nextEmail = withdrawnEmail(authUuid);
        String nextPhoneNumber = withdrawnPhoneNumber(authUuid);
        if (memberStatus == MemberStatus.WITHDRAWN
                && nextLoginId.equals(loginId)
                && nextEmail.equals(email)
                && nextPhoneNumber.equals(phoneNumber)) {
            return this;
        }
        return AuthDomain.builder()
                .authUuid(this.authUuid)
                .loginId(nextLoginId)
                .memberName(this.memberName)
                .birthdayDate(this.birthdayDate)
                .phoneNumber(nextPhoneNumber)
                .gender(this.gender)
                .email(nextEmail)
                .passwordHash(this.passwordHash)
                .passwordChangedAt(this.passwordChangedAt)
                .memberStatus(MemberStatus.WITHDRAWN)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    private static String compactUuid(String authUuid) {
        if (authUuid == null || authUuid.isBlank()) {
            throw new IllegalArgumentException("authUuid는 필수입니다.");
        }
        String compact = authUuid.trim().replace("-", "").toLowerCase(Locale.ROOT);
        if (compact.length() < WITHDRAWN_COMPACT_ID_LENGTH) {
            throw new IllegalArgumentException("authUuid 형식이 올바르지 않습니다.");
        }
        return compact;
    }

    /** 평문 비밀번호 검증 — Application에서 encode 전에 호출 */
    public static void validatePlainPassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "비밀번호는 8~20자, 영문 대문자·소문자·숫자·특수문자(!@#$%^&*()-+_=)를 각각 1자 이상 포함해야 합니다."
            );
        }
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

    @Builder(access = AccessLevel.PRIVATE)
    private AuthDomain(
            String authUuid,
            String loginId,
            String memberName,
            LocalDate birthdayDate,
            String phoneNumber,
            Gender gender,
            String email,
            String passwordHash,
            Instant passwordChangedAt,
            MemberStatus memberStatus,
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
        this.passwordHash = passwordHash;
        this.passwordChangedAt = passwordChangedAt;
        this.memberStatus = memberStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
