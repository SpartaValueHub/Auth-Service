package com.sparta.auth_service.application.support;

import java.time.Instant;

/**
 * JWT {@code expiresAt}와 현재 시각 기준 Redis TTL(초) 계산.
 * Redis가 JWT보다 먼저 만료되지 않도록 잔여 시간을 올림(CEIL)한다.
 */
public final class TokenTtlCalculator {

    private TokenTtlCalculator() {
    }

    /**
     * @param expiresAt JWT 만료 시각 (null이면 0)
     * @param now       기준 시각 (보통 {@code clock.instant()})
     * @return Redis TTL 초. {@code expiresAt <= now}이면 0, 유효하면 잔여 초 CEIL
     */
    public static long remainingTtlSeconds(Instant expiresAt, Instant now) {
        if (expiresAt == null || now == null || !expiresAt.isAfter(now)) {
            return 0L;
        }

        long epochSecondDiff = expiresAt.getEpochSecond() - now.getEpochSecond();
        if (epochSecondDiff < 0L) {
            return 0L;
        }

        int nanoDiff = expiresAt.getNano() - now.getNano();
        if (nanoDiff > 0) {
            if (epochSecondDiff == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            return epochSecondDiff + 1L;
        }
        return epochSecondDiff;
    }
}
