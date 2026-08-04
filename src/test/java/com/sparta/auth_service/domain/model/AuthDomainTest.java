package com.sparta.auth_service.domain.model;

import com.sparta.auth_service.domain.enums.Gender;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthDomainTest {

    private static final String PASSWORD_HASH = "$2a$10$encodedhashvalueplaceholder";
    private static final LocalDate BIRTHDAY = LocalDate.of(1990, 1, 1);

    @Test
    void createSignUp_success() {
        AuthDomain auth = AuthDomain.createSignUp(
                "user01",
                PASSWORD_HASH,
                "User@Example.COM",
                "홍길동",
                BIRTHDAY,
                "01012345678",
                Gender.MALE,
                "ci-value-001"
        );

        assertThat(auth.getAuthUuid()).isNotBlank();
        assertThat(auth.getLoginId()).isEqualTo("user01");
        assertThat(auth.getEmail()).isEqualTo("user@example.com");
        assertThat(auth.getMemberName()).isEqualTo("홍길동");
        assertThat(auth.getBirthdayDate()).isEqualTo(BIRTHDAY);
        assertThat(auth.getGender()).isEqualTo(Gender.MALE);
        assertThat(auth.getLoginFailCount()).isZero();
        assertThat(auth.getLockedUntil()).isNull();
        assertThat(auth.getPasswordChangedAt()).isNotNull();
        assertThat(auth.getCreatedAt()).isNull();
        assertThat(auth.getUpdatedAt()).isNull();
    }

    @Test
    void createSignUp_rejectsBlankMemberName() {
        assertThatThrownBy(() -> AuthDomain.createSignUp(
                "user01", PASSWORD_HASH, "user@example.com", "  ",
                BIRTHDAY, "01012345678", Gender.MALE, "ci-value-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memberName");
    }

    @Test
    void createSignUp_rejectsTooLongMemberName() {
        assertThatThrownBy(() -> AuthDomain.createSignUp(
                "user01", PASSWORD_HASH, "user@example.com", "a".repeat(31),
                BIRTHDAY, "01012345678", Gender.MALE, "ci-value-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memberName");
    }

    @Test
    void createSignUp_rejectsFutureBirthdayDate() {
        assertThatThrownBy(() -> AuthDomain.createSignUp(
                "user01", PASSWORD_HASH, "user@example.com", "홍길동",
                LocalDate.now().plusDays(1), "01012345678", Gender.MALE, "ci-value-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("birthdayDate");
    }

    @Test
    void createSignUp_normalizesEmailToLowerCase() {
        AuthDomain auth = AuthDomain.createSignUp(
                "user01", PASSWORD_HASH, "MixedCase@Example.COM", "홍길동",
                BIRTHDAY, "01012345678", Gender.MALE, "ci-value-001");

        assertThat(auth.getEmail()).isEqualTo("mixedcase@example.com");
    }

    @Test
    void reconstitute_preservesLoginFailCountAndLockState() {
        Instant lockedUntil = Instant.parse("2030-01-01T00:00:00Z");
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2024-06-01T00:00:00Z");
        Instant passwordChangedAt = Instant.parse("2024-03-01T00:00:00Z");

        AuthDomain auth = AuthDomain.reconstitute(
                "uuid-001",
                "user01",
                "홍길동",
                BIRTHDAY,
                "01012345678",
                Gender.MALE,
                "user@example.com",
                "ci-value-001",
                PASSWORD_HASH,
                passwordChangedAt,
                5,
                lockedUntil,
                createdAt,
                updatedAt
        );

        assertThat(auth.getLoginFailCount()).isEqualTo(5);
        assertThat(auth.getLockedUntil()).isEqualTo(lockedUntil);
        assertThat(auth.getPasswordChangedAt()).isEqualTo(passwordChangedAt);
        assertThat(auth.getCreatedAt()).isEqualTo(createdAt);
        assertThat(auth.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void changePasswordHash_changesOnlyPasswordFields() {
        Instant lockedUntil = Instant.parse("2030-01-01T00:00:00Z");
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2024-06-01T00:00:00Z");
        Instant passwordChangedAt = Instant.parse("2024-03-01T00:00:00Z");

        AuthDomain original = AuthDomain.reconstitute(
                "uuid-001",
                "user01",
                "홍길동",
                BIRTHDAY,
                "01012345678",
                Gender.MALE,
                "user@example.com",
                "ci-value-001",
                PASSWORD_HASH,
                passwordChangedAt,
                3,
                lockedUntil,
                createdAt,
                updatedAt
        );

        String newHash = "$2a$10$newencodedhashvalue1234567890";
        AuthDomain changed = original.changePasswordHash(newHash);

        assertThat(changed.getPasswordHash()).isEqualTo(newHash);
        assertThat(changed.getPasswordChangedAt()).isAfter(passwordChangedAt);
        assertThat(changed.getAuthUuid()).isEqualTo(original.getAuthUuid());
        assertThat(changed.getLoginId()).isEqualTo(original.getLoginId());
        assertThat(changed.getMemberName()).isEqualTo(original.getMemberName());
        assertThat(changed.getLoginFailCount()).isEqualTo(original.getLoginFailCount());
        assertThat(changed.getLockedUntil()).isEqualTo(original.getLockedUntil());
        assertThat(changed.getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(changed.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
    }

    @Test
    void recordLoginFailure_incrementsFailCount() {
        AuthDomain auth = createAuthWithLoginState(0, null);
        Instant now = Instant.parse("2025-01-01T00:00:00Z");

        AuthDomain failed = auth.recordLoginFailure(now);

        assertThat(failed.getLoginFailCount()).isEqualTo(1);
        assertThat(failed.getLockedUntil()).isNull();
    }

    @Test
    void recordLoginFailure_locksAccountAfterMaxFailures() {
        AuthDomain auth = createAuthWithLoginState(4, null);
        Instant now = Instant.parse("2025-01-01T00:00:00Z");

        AuthDomain failed = auth.recordLoginFailure(now);

        assertThat(failed.getLoginFailCount()).isEqualTo(5);
        assertThat(failed.getLockedUntil()).isEqualTo(now.plusSeconds(10 * 60));
        assertThat(failed.isLocked(now)).isTrue();
    }

    @Test
    void resetLoginFailure_clearsFailCountAndLock() {
        Instant lockedUntil = Instant.parse("2030-01-01T00:00:00Z");
        AuthDomain auth = createAuthWithLoginState(5, lockedUntil);

        AuthDomain reset = auth.resetLoginFailure();

        assertThat(reset.getLoginFailCount()).isZero();
        assertThat(reset.getLockedUntil()).isNull();
        assertThat(reset.getAuthUuid()).isEqualTo(auth.getAuthUuid());
    }

    @Test
    void isLocked_returnsTrueWhenLockedUntilIsAfterNow() {
        AuthDomain auth = createAuthWithLoginState(5, Instant.parse("2030-01-01T00:00:00Z"));

        assertThat(auth.isLocked(Instant.parse("2025-01-01T00:00:00Z"))).isTrue();
    }

    private AuthDomain createAuthWithLoginState(int loginFailCount, Instant lockedUntil) {
        return AuthDomain.reconstitute(
                "uuid-001",
                "user01",
                "홍길동",
                BIRTHDAY,
                "01012345678",
                Gender.MALE,
                "user@example.com",
                "ci-value-001",
                PASSWORD_HASH,
                Instant.parse("2024-03-01T00:00:00Z"),
                loginFailCount,
                lockedUntil,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-06-01T00:00:00Z")
        );
    }
}
