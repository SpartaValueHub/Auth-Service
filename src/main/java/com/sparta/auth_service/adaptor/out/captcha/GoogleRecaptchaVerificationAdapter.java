package com.sparta.auth_service.adaptor.out.captcha;

import com.sparta.auth_service.application.exception.CaptchaProviderUnavailableException;
import com.sparta.auth_service.application.port.out.CaptchaVerificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.Optional;

/**
 * Google reCAPTCHA v2 Checkbox siteverify.
 * <p>
 * 사용자 검증 실패 → false. 제공자 장애 → {@link CaptchaProviderUnavailableException}(fail-closed, 503).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleRecaptchaVerificationAdapter implements CaptchaVerificationPort {

    private static final String SITEVERIFY_URI = "https://www.google.com/recaptcha/api/siteverify";

    private final CaptchaProperties captchaProperties;
    private final RecaptchaVerificationEvaluator verificationEvaluator;
    private final Clock clock;
    private final RestClient recaptchaRestClient;

    @Override
    public boolean verify(String captchaToken) {
        if (!captchaProperties.isEnabled()) {
            return true;
        }
        if (captchaToken == null || captchaToken.isBlank()) {
            log.warn("captcha verification failed: reason=blank_token");
            return false;
        }

        try {
            RecaptchaSiteVerifyResponse response = callSiteVerify(captchaToken.trim());
            Optional<RecaptchaVerificationEvaluator.FailReason> failReason = verificationEvaluator.evaluate(
                    response,
                    captchaProperties.normalizedAllowedHostnames(),
                    captchaProperties.getRecaptcha().getChallengeMaxAgeSeconds(),
                    clock
            );
            if (failReason.isPresent()) {
                log.warn("captcha verification failed: reason={}", failReason.get());
                return false;
            }
            return true;
        } catch (CaptchaProviderUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            if (isProviderFailure(ex)) {
                log.warn("captcha provider unavailable: reason={} exception={}",
                        resolveFailureReason(ex), ex.getClass().getSimpleName());
                log.debug("captcha siteverify provider failure", ex);
                throw new CaptchaProviderUnavailableException(ex);
            }
            log.warn("captcha verification failed: reason={} exception={}",
                    resolveFailureReason(ex), ex.getClass().getSimpleName());
            log.debug("captcha siteverify failed", ex);
            return false;
        }
    }

    private RecaptchaSiteVerifyResponse callSiteVerify(String captchaToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", captchaProperties.getRecaptcha().getSecretKey());
        form.add("response", captchaToken);

        return recaptchaRestClient.post()
                .uri(SITEVERIFY_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(RecaptchaSiteVerifyResponse.class);
    }

    private static boolean isProviderFailure(Throwable ex) {
        if (hasCauseOfType(ex, HttpMessageNotReadableException.class)) {
            return true;
        }
        if (hasTransportFailureCause(ex)) {
            return true;
        }
        RestClientResponseException responseEx = findCauseOfType(ex, RestClientResponseException.class);
        if (responseEx != null) {
            return isProviderFailureStatus(responseEx.getStatusCode().value());
        }
        return false;
    }

    private static boolean isProviderFailureStatus(int status) {
        return status >= 500
                || status == 429
                || status == 400
                || status == 401
                || status == 403;
    }

    private static String resolveFailureReason(Throwable ex) {
        if (hasCauseOfType(ex, HttpMessageNotReadableException.class)) {
            return "siteverify_parse_failed";
        }
        if (hasCauseOfType(ex, UnknownHostException.class)) {
            return "siteverify_dns_failed";
        }
        if (hasCauseOfType(ex, SSLException.class)) {
            return "siteverify_ssl_failed";
        }
        if (hasCauseOfType(ex, SocketTimeoutException.class)) {
            return "siteverify_timeout";
        }
        if (hasCauseOfType(ex, ConnectException.class)) {
            return "siteverify_connect_failed";
        }
        if (hasCauseOfType(ex, SocketException.class)) {
            return "siteverify_connection_failed";
        }
        if (hasCauseOfType(ex, ResourceAccessException.class)) {
            return "siteverify_transport_failed";
        }
        RestClientResponseException responseEx = findCauseOfType(ex, RestClientResponseException.class);
        if (responseEx != null) {
            int status = responseEx.getStatusCode().value();
            if (status >= 500) {
                return "siteverify_server_error";
            }
            if (status == 429) {
                return "siteverify_rate_limited";
            }
            if (status == 400 || status == 401 || status == 403) {
                return "siteverify_client_error";
            }
        }
        return "siteverify_request_failed";
    }

    private static boolean hasCauseOfType(Throwable ex, Class<? extends Throwable> type) {
        return findCauseOfType(ex, type) != null;
    }

    private static <T extends Throwable> T findCauseOfType(Throwable ex, Class<T> type) {
        Throwable current = ex;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private static boolean hasTransportFailureCause(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ResourceAccessException
                    || current instanceof UnknownHostException
                    || current instanceof SocketException
                    || current instanceof SocketTimeoutException
                    || current instanceof ConnectException
                    || current instanceof SSLException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
