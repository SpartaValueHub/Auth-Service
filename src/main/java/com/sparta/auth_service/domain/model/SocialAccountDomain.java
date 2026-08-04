package com.sparta.auth_service.domain.model;

import com.sparta.auth_service.domain.enums.SocialProvider;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 소셜 계정 연동 도메인 — provider + providerUserId로 외부 계정 식별.
 * authUuid로 AuthDomain과 연결; 동일 provider 계정 중복 연동은 Application·DB 제약으로 방지.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccountDomain {

    private Long socialAccountId;
    /** 연동 대상 AuthDomain 식별자 */
    private String authUuid;
    private SocialProvider provider;
    /** 제공자 측 사용자 ID — provider와 함께 유일 */
    private String providerUserId;
    private String providerEmail;
    private Instant linkedAt;
    private Instant createdAt;

    /** 신규 연동 — linkedAt을 Domain에서 설정 */
    public static SocialAccountDomain link(
            String authUuid,
            SocialProvider provider,
            String providerUserId,
            String providerEmail
    ) {
        validateAuthUuid(authUuid);
        validateProvider(provider);
        validateProviderUserId(providerUserId);

        Instant now = Instant.now();
        return SocialAccountDomain.builder()
                .authUuid(authUuid)
                .provider(provider)
                .providerUserId(providerUserId.trim())
                .providerEmail(providerEmail == null ? null : providerEmail.trim())
                .linkedAt(now)
                .build();
    }

    public static SocialAccountDomain reconstitute(
            Long socialAccountId,
            String authUuid,
            SocialProvider provider,
            String providerUserId,
            String providerEmail,
            Instant linkedAt,
            Instant createdAt
    ) {
        return SocialAccountDomain.builder()
                .socialAccountId(socialAccountId)
                .authUuid(authUuid)
                .provider(provider)
                .providerUserId(providerUserId)
                .providerEmail(providerEmail)
                .linkedAt(linkedAt)
                .createdAt(createdAt)
                .build();
    }

    private static void validateAuthUuid(String authUuid) {
        if (authUuid == null || authUuid.isBlank()) {
            throw new IllegalArgumentException("authUuid는 필수입니다.");
        }
    }

    private static void validateProvider(SocialProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider는 필수입니다.");
        }
    }

    private static void validateProviderUserId(String providerUserId) {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("providerUserId는 필수입니다.");
        }
    }

    @Builder(access = AccessLevel.PRIVATE)
    private SocialAccountDomain(
            Long socialAccountId,
            String authUuid,
            SocialProvider provider,
            String providerUserId,
            String providerEmail,
            Instant linkedAt,
            Instant createdAt
    ) {
        this.socialAccountId = socialAccountId;
        this.authUuid = authUuid;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
        this.linkedAt = linkedAt;
        this.createdAt = createdAt;
    }
}
