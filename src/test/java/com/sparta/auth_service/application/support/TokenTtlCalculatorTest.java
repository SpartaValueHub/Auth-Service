package com.sparta.auth_service.application.support;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TokenTtlCalculatorTest {

    private static final Instant NOW = Instant.parse("2025-06-01T12:00:00Z");

    @Test
    void returnsZeroWhenExpiresAtIsNull() {
        assertThat(TokenTtlCalculator.remainingTtlSeconds(null, NOW)).isZero();
    }

    @Test
    void returnsZeroWhenExpiresAtEqualsNow() {
        assertThat(TokenTtlCalculator.remainingTtlSeconds(NOW, NOW)).isZero();
    }

    @Test
    void returnsZeroWhenExpiresAtIsBeforeNow() {
        assertThat(TokenTtlCalculator.remainingTtlSeconds(NOW.minusSeconds(1), NOW)).isZero();
    }

    @Test
    void ceilsFractionalSecondToNextWholeSecond() {
        Instant expiresAt = NOW.plusSeconds(900).plusNanos(1);
        assertThat(TokenTtlCalculator.remainingTtlSeconds(expiresAt, NOW)).isEqualTo(901L);
    }

    @Test
    void ceilsSubSecondRemainderToOneSecond() {
        Instant expiresAt = NOW.plusNanos(1_000_000);
        assertThat(TokenTtlCalculator.remainingTtlSeconds(expiresAt, NOW)).isEqualTo(1L);
    }

    @Test
    void returnsExactSecondsWhenNoFractionalRemainder() {
        Instant expiresAt = NOW.plusSeconds(900);
        assertThat(TokenTtlCalculator.remainingTtlSeconds(expiresAt, NOW)).isEqualTo(900L);
    }

    @Test
    void ceilsWhenNanoDiffNegativeWithinSameSecondBoundary() {
        Instant now = NOW.plusNanos(500_000_000);
        Instant expiresAt = NOW.plusSeconds(1).plusNanos(200_000_000);
        assertThat(TokenTtlCalculator.remainingTtlSeconds(expiresAt, now)).isEqualTo(1L);
    }

    @Test
    void returnsZeroWhenNowIsNull() {
        assertThat(TokenTtlCalculator.remainingTtlSeconds(NOW.plusSeconds(60), null)).isZero();
    }
}
