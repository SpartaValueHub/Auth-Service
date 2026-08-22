package com.sparta.auth_service.domain.model;

import com.sparta.auth_service.domain.enums.Gender;
import com.sparta.auth_service.domain.enums.MemberStatus;
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
                Gender.MALE
        );

        assertThat(auth.getAuthUuid()).isNotBlank();
        assertThat(auth.getLoginId()).isEqualTo("user01");
        assertThat(auth.getEmail()).isEqualTo("user@example.com");
        assertThat(auth.getMemberName()).isEqualTo("홍길동");
        assertThat(auth.getBirthdayDate()).isEqualTo(BIRTHDAY);
        assertThat(auth.getGender()).isEqualTo(Gender.MALE);
        assertThat(auth.getMemberStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(auth.isActive()).isTrue();
        assertThat(auth.getPasswordChangedAt()).isNotNull();
        assertThat(auth.getCreatedAt()).isNull();
        assertThat(auth.getUpdatedAt()).isNull();
    }

    @Test
    void createSignUp_rejectsBlankMemberName() {
        assertThatThrownBy(() -> AuthDomain.createSignUp(
                "user01", PASSWORD_HASH, "user@example.com", "  ",
                BIRTHDAY, "01012345678", Gender.MALE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memberName");
    }

    @Test
    void createSignUp_rejectsTooLongMemberName() {
        assertThatThrownBy(() -> AuthDomain.createSignUp(
                "user01", PASSWORD_HASH, "user@example.com", "a".repeat(31),
                BIRTHDAY, "01012345678", Gender.MALE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memberName");
    }

    @Test
    void createSignUp_rejectsFutureBirthdayDate() {
        assertThatThrownBy(() -> AuthDomain.createSignUp(
                "user01", PASSWORD_HASH, "user@example.com", "홍길동",
                LocalDate.now().plusDays(1), "01012345678", Gender.MALE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("birthdayDate");
    }

    @Test
    void createSignUp_normalizesEmailToLowerCase() {
        AuthDomain auth = AuthDomain.createSignUp(
                "user01", PASSWORD_HASH, "MixedCase@Example.COM", "홍길동",
                BIRTHDAY, "01012345678", Gender.MALE);

        assertThat(auth.getEmail()).isEqualTo("mixedcase@example.com");
    }

    @Test
    void createSignUp_trimsLoginId() {
        AuthDomain auth = AuthDomain.createSignUp(
                "  user01  ", PASSWORD_HASH, "user@example.com", "홍길동",
                BIRTHDAY, "01012345678", Gender.MALE);

        assertThat(auth.getLoginId()).isEqualTo("user01");
    }

    @Test
    void createSignUp_trimsPhoneNumber() {
        AuthDomain auth = AuthDomain.createSignUp(
                "user01", PASSWORD_HASH, "user@example.com", "홍길동",
                BIRTHDAY, "  01012345678  ", Gender.MALE);

        assertThat(auth.getPhoneNumber()).isEqualTo("01012345678");
    }

    @Test
    void normalizeEmailForLookup_matchesCreateSignUpStoredValue() {
        AuthDomain auth = AuthDomain.createSignUp(
                "user01", PASSWORD_HASH, "  MixedCase@Example.COM  ", "홍길동",
                BIRTHDAY, "01012345678", Gender.MALE);

        assertThat(AuthDomain.normalizeEmailForLookup("  MixedCase@Example.COM  "))
                .isEqualTo(auth.getEmail());
    }

    @Test
    void normalizeLoginIdForLookup_matchesCreateSignUpStoredValue() {
        AuthDomain auth = AuthDomain.createSignUp(
                "  user01  ", PASSWORD_HASH, "user@example.com", "홍길동",
                BIRTHDAY, "01012345678", Gender.MALE);

        assertThat(AuthDomain.normalizeLoginIdForLookup("  user01  "))
                .isEqualTo(auth.getLoginId());
    }

    @Test
    void normalizePhoneNumberForLookup_matchesCreateSignUpStoredValue() {
        AuthDomain auth = AuthDomain.createSignUp(
                "user01", PASSWORD_HASH, "user@example.com", "홍길동",
                BIRTHDAY, "  01012345678  ", Gender.MALE);

        assertThat(AuthDomain.normalizePhoneNumberForLookup("  01012345678  "))
                .isEqualTo(auth.getPhoneNumber());
    }

    @Test
    void reconstitute_preservesMemberStatus() {
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
                PASSWORD_HASH,
                passwordChangedAt,
                MemberStatus.SUSPENDED,
                createdAt,
                updatedAt
        );

        assertThat(auth.getMemberStatus()).isEqualTo(MemberStatus.SUSPENDED);
        assertThat(auth.isActive()).isFalse();
        assertThat(auth.getPasswordChangedAt()).isEqualTo(passwordChangedAt);
        assertThat(auth.getCreatedAt()).isEqualTo(createdAt);
        assertThat(auth.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void withdraw_transitionsActiveToWithdrawnAndAnonymizesIdentifiers() {
        AuthDomain auth = AuthDomain.createSignUp(
                "user01", PASSWORD_HASH, "user@example.com", "홍길동",
                BIRTHDAY, "01012345678", Gender.MALE
        );

        AuthDomain withdrawn = auth.withdraw();

        assertThat(withdrawn.isWithdrawn()).isTrue();
        assertThat(withdrawn.isActive()).isFalse();
        assertThat(withdrawn.getMemberStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(withdrawn.getAuthUuid()).isEqualTo(auth.getAuthUuid());
        assertThat(withdrawn.getLoginId()).isNotEqualTo(auth.getLoginId());
        assertThat(withdrawn.getEmail()).isNotEqualTo(auth.getEmail());
        assertThat(withdrawn.getPhoneNumber()).isNotEqualTo(auth.getPhoneNumber());
        assertAnonymizedFromAuthUuid(withdrawn, auth.getAuthUuid());
    }

    @Test
    void withdraw_isIdempotentWhenAlreadyAnonymized() {
        AuthDomain withdrawn = AuthDomain.createSignUp(
                "user01", PASSWORD_HASH, "user@example.com", "홍길동",
                BIRTHDAY, "01012345678", Gender.MALE
        ).withdraw();

        assertThat(withdrawn.withdraw()).isSameAs(withdrawn);
    }

    @Test
    void withdraw_anonymizesLegacyWithdrawnRowStillHoldingIdentifiers() {
        String authUuid = "550e8400-e29b-41d4-a716-446655440000";
        AuthDomain legacyWithdrawn = AuthDomain.reconstitute(
                authUuid, "user01", "홍길동", BIRTHDAY, "01012345678", Gender.MALE,
                "user@example.com", PASSWORD_HASH, Instant.parse("2026-01-01T00:00:00Z"),
                MemberStatus.WITHDRAWN, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z")
        );

        AuthDomain anonymized = legacyWithdrawn.withdraw();

        assertThat(anonymized).isNotSameAs(legacyWithdrawn);
        assertThat(anonymized.getLoginId()).isNotEqualTo("user01");
        assertThat(anonymized.getEmail()).isNotEqualTo("user@example.com");
        assertThat(anonymized.getPhoneNumber()).isNotEqualTo("01012345678");
        assertAnonymizedFromAuthUuid(anonymized, authUuid);
    }

    @Test
    void withdraw_rejectsNonActiveStatus() {
        AuthDomain suspended = AuthDomain.reconstitute(
                "uuid-001", "user01", "홍길동", BIRTHDAY, "01012345678", Gender.MALE,
                "user@example.com", PASSWORD_HASH, Instant.parse("2026-01-01T00:00:00Z"),
                MemberStatus.SUSPENDED, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z")
        );

        assertThatThrownBy(suspended::withdraw)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("탈퇴할 수 없는");
    }

    // withdraw() 결과만 검증 — Domain 내부 anonymize 헬퍼는 public API가 아님
    private static void assertAnonymizedFromAuthUuid(AuthDomain withdrawn, String authUuid) {
        String compact = authUuid.replace("-", "").toLowerCase();
        assertThat(withdrawn.getLoginId()).isEqualTo("w" + compact.substring(0, 19));
        assertThat(withdrawn.getPhoneNumber()).isEqualTo("w" + compact.substring(0, 19));
        assertThat(withdrawn.getEmail()).isEqualTo("w" + compact + "@w.invalid");
    }
}
