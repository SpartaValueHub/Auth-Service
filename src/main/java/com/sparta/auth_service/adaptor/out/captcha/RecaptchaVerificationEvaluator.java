package com.sparta.auth_service.adaptor.out.captcha;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** siteverify 응답 success·hostname·challenge_ts 검증 — reCAPTCHA v2 전용 */
@Component
class RecaptchaVerificationEvaluator {

    enum FailReason {
        RESPONSE_NULL,
        SUCCESS_FALSE,
        HOSTNAME_MISSING,
        HOSTNAME_NOT_ALLOWED,
        CHALLENGE_TS_MISSING,
        CHALLENGE_TS_PARSE_FAILED,
        CHALLENGE_TS_FUTURE,
        CHALLENGE_TS_EXPIRED
    }

    Optional<FailReason> evaluate(
            RecaptchaSiteVerifyResponse response,
            Set<String> allowedHostnames,
            int challengeMaxAgeSeconds,
            Clock clock
    ) {
        if (response == null) {
            return Optional.of(FailReason.RESPONSE_NULL);
        }
        if (!response.isSuccess()) {
            return Optional.of(FailReason.SUCCESS_FALSE);
        }

        if (!StringUtils.hasText(response.getHostname())) {
            return Optional.of(FailReason.HOSTNAME_MISSING);
        }
        String normalizedHostname = response.getHostname().trim().toLowerCase(Locale.ROOT);
        if (allowedHostnames.isEmpty() || !allowedHostnames.contains(normalizedHostname)) {
            return Optional.of(FailReason.HOSTNAME_NOT_ALLOWED);
        }

        if (!StringUtils.hasText(response.getChallengeTs())) {
            return Optional.of(FailReason.CHALLENGE_TS_MISSING);
        }

        Instant challengeInstant;
        try {
            challengeInstant = Instant.parse(response.getChallengeTs().trim());
        } catch (DateTimeParseException ex) {
            return Optional.of(FailReason.CHALLENGE_TS_PARSE_FAILED);
        }

        Instant now = clock.instant();
        if (challengeInstant.isAfter(now)) {
            return Optional.of(FailReason.CHALLENGE_TS_FUTURE);
        }
        if (Duration.between(challengeInstant, now).getSeconds() > challengeMaxAgeSeconds) {
            return Optional.of(FailReason.CHALLENGE_TS_EXPIRED);
        }

        return Optional.empty();
    }
}
