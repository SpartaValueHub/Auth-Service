package com.sparta.auth_service.adaptor.out.captcha;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static com.sparta.auth_service.adaptor.out.captcha.RecaptchaVerificationEvaluator.FailReason;
import static org.assertj.core.api.Assertions.assertThat;

class RecaptchaVerificationEvaluatorTest {

    private static final Instant NOW = Instant.parse("2026-08-05T10:00:00Z");
    private static final Set<String> ALLOWED = Set.of("localhost", "127.0.0.1", "valuehub.example.com");

    private final RecaptchaVerificationEvaluator evaluator = new RecaptchaVerificationEvaluator();
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Test
    void acceptsValidResponse() {
        RecaptchaSiteVerifyResponse response = validResponse("localhost", "2026-08-05T09:59:30Z");

        assertThat(evaluator.evaluate(response, ALLOWED, 120, clock)).isEmpty();
    }

    @Test
    void acceptsHostnameCaseInsensitively() {
        RecaptchaSiteVerifyResponse response = validResponse("LocalHost", "2026-08-05T09:59:30Z");

        assertThat(evaluator.evaluate(response, ALLOWED, 120, clock)).isEmpty();
    }

    @Test
    void acceptsChallengeTsAtMaxAgeBoundary() {
        RecaptchaSiteVerifyResponse response = validResponse("localhost", "2026-08-05T09:58:00Z");

        assertThat(evaluator.evaluate(response, ALLOWED, 120, clock)).isEmpty();
    }

    @Test
    void rejectsNullResponse() {
        assertThat(evaluator.evaluate(null, ALLOWED, 120, clock))
                .contains(FailReason.RESPONSE_NULL);
    }

    @Test
    void rejectsSuccessFalse() {
        RecaptchaSiteVerifyResponse response = validResponse("localhost", "2026-08-05T09:59:30Z");
        response.setSuccess(false);

        assertThat(evaluator.evaluate(response, ALLOWED, 120, clock))
                .contains(FailReason.SUCCESS_FALSE);
    }

    @Test
    void rejectsMissingHostname() {
        RecaptchaSiteVerifyResponse response = validResponse(null, "2026-08-05T09:59:30Z");

        assertThat(evaluator.evaluate(response, ALLOWED, 120, clock))
                .contains(FailReason.HOSTNAME_MISSING);
    }

    @Test
    void rejectsHostnameNotInAllowlist() {
        RecaptchaSiteVerifyResponse response = validResponse("evil.example.com", "2026-08-05T09:59:30Z");

        assertThat(evaluator.evaluate(response, ALLOWED, 120, clock))
                .contains(FailReason.HOSTNAME_NOT_ALLOWED);
    }

    @Test
    void rejectsHostnameSuffixBypass() {
        RecaptchaSiteVerifyResponse response = validResponse("evillocalhost", "2026-08-05T09:59:30Z");

        assertThat(evaluator.evaluate(response, ALLOWED, 120, clock))
                .contains(FailReason.HOSTNAME_NOT_ALLOWED);
    }

    @Test
    void rejectsWhenAllowlistEmpty() {
        RecaptchaSiteVerifyResponse response = validResponse("localhost", "2026-08-05T09:59:30Z");

        assertThat(evaluator.evaluate(response, Set.of(), 120, clock))
                .contains(FailReason.HOSTNAME_NOT_ALLOWED);
    }

    @Test
    void rejectsLocalhostWhenNotConfigured() {
        RecaptchaSiteVerifyResponse response = validResponse("localhost", "2026-08-05T09:59:30Z");

        assertThat(evaluator.evaluate(response, Set.of("valuehub.example.com"), 120, clock))
                .contains(FailReason.HOSTNAME_NOT_ALLOWED);
    }

    @Test
    void rejectsMissingChallengeTs() {
        RecaptchaSiteVerifyResponse response = validResponse("localhost", null);

        assertThat(evaluator.evaluate(response, ALLOWED, 120, clock))
                .contains(FailReason.CHALLENGE_TS_MISSING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-timestamp", "2026/08/05 10:00:00"})
    void rejectsUnparseableChallengeTs(String challengeTs) {
        RecaptchaSiteVerifyResponse response = validResponse("localhost", challengeTs);

        assertThat(evaluator.evaluate(response, ALLOWED, 120, clock))
                .contains(FailReason.CHALLENGE_TS_PARSE_FAILED);
    }

    @Test
    void rejectsFutureChallengeTs() {
        RecaptchaSiteVerifyResponse response = validResponse("localhost", "2026-08-05T10:00:01Z");

        assertThat(evaluator.evaluate(response, ALLOWED, 120, clock))
                .contains(FailReason.CHALLENGE_TS_FUTURE);
    }

    @Test
    void rejectsExpiredChallengeTs() {
        RecaptchaSiteVerifyResponse response = validResponse("localhost", "2026-08-05T09:57:59Z");

        assertThat(evaluator.evaluate(response, ALLOWED, 120, clock))
                .contains(FailReason.CHALLENGE_TS_EXPIRED);
    }

    private static RecaptchaSiteVerifyResponse validResponse(String hostname, String challengeTs) {
        RecaptchaSiteVerifyResponse response = new RecaptchaSiteVerifyResponse();
        response.setSuccess(true);
        response.setHostname(hostname);
        response.setChallengeTs(challengeTs);
        return response;
    }
}
