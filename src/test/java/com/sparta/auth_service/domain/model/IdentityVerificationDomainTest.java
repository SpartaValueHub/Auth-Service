package com.sparta.auth_service.domain.model;

import com.sparta.auth_service.domain.enums.VerificationMethod;
import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.enums.VerificationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityVerificationDomainTest {

    private static final Instant VERIFIED_AT = Instant.parse("2025-01-01T00:00:00Z");
    private static final String CI_HASH = "ci-hash-001";
    private static final String REQUEST_TOKEN = "verify-001";

    @Test
    void createRequested_success() {
        IdentityVerificationDomain domain = IdentityVerificationDomain.createRequested(
                REQUEST_TOKEN,
                VerificationPurpose.SIGN_UP
        );

        assertThat(domain.getVerificationUuid()).isNotBlank();
        assertThat(domain.getRequestToken()).isEqualTo(REQUEST_TOKEN);
        assertThat(domain.getPurpose()).isEqualTo(VerificationPurpose.SIGN_UP);
        assertThat(domain.getVerificationStatus()).isEqualTo(VerificationStatus.REQUESTED);
        assertThat(domain.getMemberUuid()).isNull();
        assertThat(domain.getVerificationMethod()).isNull();
        assertThat(domain.getCiHash()).isNull();
        assertThat(domain.getVerifiedAt()).isNull();
    }

    @Test
    void createRequested_rejectsBlankRequestToken() {
        assertThatThrownBy(() -> IdentityVerificationDomain.createRequested("  ", VerificationPurpose.SIGN_UP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requestToken");
    }

    @Test
    void markVerified_transitionsRequestedToSuccess() {
        IdentityVerificationDomain requested = createRequested();
        IdentityVerificationDomain verified = requested.markVerified(
                VerificationMethod.PASS,
                CI_HASH,
                VERIFIED_AT
        );

        assertThat(verified.getVerificationStatus()).isEqualTo(VerificationStatus.SUCCESS);
        assertThat(verified.getVerificationMethod()).isEqualTo(VerificationMethod.PASS);
        assertThat(verified.getCiHash()).isEqualTo(CI_HASH);
        assertThat(verified.getVerifiedAt()).isEqualTo(VERIFIED_AT);
        assertThat(requested.getVerificationStatus()).isEqualTo(VerificationStatus.REQUESTED);
    }

    @Test
    void markVerified_rejectsWhenNotRequested() {
        IdentityVerificationDomain verified = createRequested().markVerified(
                VerificationMethod.PASS,
                CI_HASH,
                VERIFIED_AT
        );
        IdentityVerificationDomain failed = createRequested().markFailed();

        assertThatThrownBy(() -> verified.markVerified(VerificationMethod.PASS, CI_HASH, VERIFIED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("요청 상태의 인증만 성공 처리할 수 있습니다.");

        assertThatThrownBy(() -> failed.markVerified(VerificationMethod.PASS, CI_HASH, VERIFIED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("요청 상태의 인증만 성공 처리할 수 있습니다.");
    }

    @Test
    void markVerified_rejectsMissingFields() {
        IdentityVerificationDomain requested = createRequested();

        assertThatThrownBy(() -> requested.markVerified(null, CI_HASH, VERIFIED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verificationMethod");

        assertThatThrownBy(() -> requested.markVerified(VerificationMethod.PASS, "  ", VERIFIED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ciHash");

        assertThatThrownBy(() -> requested.markVerified(VerificationMethod.PASS, CI_HASH, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verifiedAt");
    }

    @Test
    void markFailed_transitionsRequestedToFailedAndClearsVerificationFields() {
        IdentityVerificationDomain requested = createRequested();
        IdentityVerificationDomain failed = requested.markFailed();

        assertThat(failed.getVerificationStatus()).isEqualTo(VerificationStatus.FAILED);
        assertThat(failed.getVerificationMethod()).isNull();
        assertThat(failed.getCiHash()).isNull();
        assertThat(failed.getVerifiedAt()).isNull();
        assertThat(requested.getVerificationStatus()).isEqualTo(VerificationStatus.REQUESTED);
    }

    @Test
    void markFailed_rejectsWhenNotRequested() {
        IdentityVerificationDomain verified = createRequested().markVerified(
                VerificationMethod.PASS,
                CI_HASH,
                VERIFIED_AT
        );
        IdentityVerificationDomain failed = createRequested().markFailed();

        assertThatThrownBy(verified::markFailed)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("요청 상태의 인증만 실패 처리할 수 있습니다.");

        assertThatThrownBy(failed::markFailed)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("요청 상태의 인증만 실패 처리할 수 있습니다.");
    }

    @Test
    void withMemberUuid_linksSuccessfulVerification() {
        IdentityVerificationDomain verified = createRequested().markVerified(
                VerificationMethod.PASS,
                CI_HASH,
                VERIFIED_AT
        );
        IdentityVerificationDomain linked = verified.withMemberUuid("member-uuid-001");

        assertThat(linked.getMemberUuid()).isEqualTo("member-uuid-001");
        assertThat(linked.getVerificationStatus()).isEqualTo(VerificationStatus.SUCCESS);
        assertThat(verified.getMemberUuid()).isNull();
    }

    @Test
    void withMemberUuid_rejectsWhenNotSuccessful() {
        IdentityVerificationDomain requested = createRequested();

        assertThatThrownBy(() -> requested.withMemberUuid("member-uuid-001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("성공한 인증만 회원과 연결할 수 있습니다.");
    }

    @Test
    void withMemberUuid_rejectsRelink() {
        IdentityVerificationDomain linked = createRequested()
                .markVerified(VerificationMethod.PASS, CI_HASH, VERIFIED_AT)
                .withMemberUuid("member-uuid-001");

        assertThatThrownBy(() -> linked.withMemberUuid("member-uuid-002"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 회원과 연결된 인증입니다.");
    }

    @Test
    void withMemberUuid_rejectsBlankMemberUuid() {
        IdentityVerificationDomain verified = createRequested().markVerified(
                VerificationMethod.PASS,
                CI_HASH,
                VERIFIED_AT
        );

        assertThatThrownBy(() -> verified.withMemberUuid(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memberUuid");

        assertThatThrownBy(() -> verified.withMemberUuid("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memberUuid");
    }

    @Test
    void isAvailableForSignUp_requiresSignUpPurpose() {
        IdentityVerificationDomain signUpVerified = createRequested().markVerified(
                VerificationMethod.PASS,
                CI_HASH,
                VERIFIED_AT
        );
        IdentityVerificationDomain otherPurposeVerified = IdentityVerificationDomain.createRequested(
                REQUEST_TOKEN,
                VerificationPurpose.RESET_PASSWORD
        ).markVerified(VerificationMethod.PASS, CI_HASH, VERIFIED_AT);

        assertThat(signUpVerified.isAvailableForSignUp()).isTrue();
        assertThat(otherPurposeVerified.isAvailableForSignUp()).isFalse();
    }

    @Test
    void isAvailableForSignUp_requiresSuccessUnlinkedAndCiHash() {
        IdentityVerificationDomain verified = createRequested().markVerified(
                VerificationMethod.PASS,
                CI_HASH,
                VERIFIED_AT
        );

        assertThat(verified.isAvailableForSignUp()).isTrue();
        assertThat(createRequested().isAvailableForSignUp()).isFalse();
        assertThat(verified.withMemberUuid("member-uuid-001").isAvailableForSignUp()).isFalse();

        IdentityVerificationDomain withoutCiHash = IdentityVerificationDomain.reconstitute(
                1L,
                "uuid-001",
                null,
                VerificationPurpose.SIGN_UP,
                REQUEST_TOKEN,
                VerificationMethod.PASS,
                null,
                VerificationStatus.SUCCESS,
                VERIFIED_AT,
                Instant.parse("2024-01-01T00:00:00Z")
        );
        assertThat(withoutCiHash.isAvailableForSignUp()).isFalse();
    }

    @Test
    void reconstitute_preservesStoredValuesWithoutTransformation() {
        Instant createdAt = Instant.parse("2024-06-01T00:00:00Z");

        IdentityVerificationDomain domain = IdentityVerificationDomain.reconstitute(
                10L,
                "stored-uuid",
                "linked-member",
                VerificationPurpose.FIND_ID,
                "stored-token",
                VerificationMethod.KAKAO,
                CI_HASH,
                VerificationStatus.SUCCESS,
                VERIFIED_AT,
                createdAt
        );

        assertThat(domain.getIdentityVerificationId()).isEqualTo(10L);
        assertThat(domain.getVerificationUuid()).isEqualTo("stored-uuid");
        assertThat(domain.getMemberUuid()).isEqualTo("linked-member");
        assertThat(domain.getPurpose()).isEqualTo(VerificationPurpose.FIND_ID);
        assertThat(domain.getRequestToken()).isEqualTo("stored-token");
        assertThat(domain.getVerificationMethod()).isEqualTo(VerificationMethod.KAKAO);
        assertThat(domain.getCiHash()).isEqualTo(CI_HASH);
        assertThat(domain.getVerificationStatus()).isEqualTo(VerificationStatus.SUCCESS);
        assertThat(domain.getVerifiedAt()).isEqualTo(VERIFIED_AT);
        assertThat(domain.getCreatedAt()).isEqualTo(createdAt);
    }

    private IdentityVerificationDomain createRequested() {
        return IdentityVerificationDomain.createRequested(REQUEST_TOKEN, VerificationPurpose.SIGN_UP);
    }
}
