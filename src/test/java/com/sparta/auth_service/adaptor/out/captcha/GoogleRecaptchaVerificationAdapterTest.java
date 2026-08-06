package com.sparta.auth_service.adaptor.out.captcha;

import com.sparta.auth_service.application.exception.CaptchaProviderUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(OutputCaptureExtension.class)
class GoogleRecaptchaVerificationAdapterTest {

    private static final String SITEVERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final Instant NOW = Instant.parse("2026-08-05T10:00:00Z");

    private CaptchaProperties captchaProperties;
    private GoogleRecaptchaVerificationAdapter adapter;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();

        captchaProperties = new CaptchaProperties(
                true,
                2000,
                3000,
                new CaptchaProperties.Recaptcha("test-secret", "localhost,127.0.0.1", 120)
        );

        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        adapter = new GoogleRecaptchaVerificationAdapter(
                captchaProperties,
                new RecaptchaVerificationEvaluator(),
                clock,
                restClient
        );
    }

    @Test
    void verifyReturnsTrueWhenCaptchaDisabled() {
        GoogleRecaptchaVerificationAdapter disabledAdapter = new GoogleRecaptchaVerificationAdapter(
                new CaptchaProperties(
                        false,
                        2000,
                        3000,
                        new CaptchaProperties.Recaptcha("test-secret", "localhost,127.0.0.1", 120)
                ),
                new RecaptchaVerificationEvaluator(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                RestClient.builder().build()
        );

        assertThat(disabledAdapter.verify("any-token")).isTrue();
    }

    @Test
    void verifyReturnsFalseForBlankToken() {
        assertThat(adapter.verify("   ")).isFalse();
        mockServer.verify();
    }

    @Test
    void verifyReturnsTrueForValidSiteverifyResponse() {
        mockServer.expect(requestTo(SITEVERIFY_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(validSiteverifyJson("localhost", "2026-08-05T09:59:00Z"), MediaType.APPLICATION_JSON));

        assertThat(adapter.verify("response-token")).isTrue();
        mockServer.verify();
    }

    @Test
    void verifyReturnsFalseWhenHostnameNotAllowed() {
        mockServer.expect(requestTo(SITEVERIFY_URL))
                .andRespond(withSuccess(validSiteverifyJson("evil.example.com", "2026-08-05T09:59:00Z"), MediaType.APPLICATION_JSON));

        assertThat(adapter.verify("response-token")).isFalse();
        mockServer.verify();
    }

    @Test
    void verifyReturnsFalseWhenSuccessFalse() {
        mockServer.expect(requestTo(SITEVERIFY_URL))
                .andRespond(withSuccess("""
                        {
                          "success": false,
                          "error-codes": ["invalid-input-response"]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(adapter.verify("response-token")).isFalse();
        mockServer.verify();
    }

    @Test
    void verifyReturnsFalseWhenChallengeTsExpired() {
        mockServer.expect(requestTo(SITEVERIFY_URL))
                .andRespond(withSuccess(validSiteverifyJson("localhost", "2026-08-05T09:57:59Z"), MediaType.APPLICATION_JSON));

        assertThat(adapter.verify("response-token")).isFalse();
        mockServer.verify();
    }

    @Test
    void verifyThrowsOnServerError() {
        mockServer.expect(requestTo(SITEVERIFY_URL))
                .andRespond(withServerError());

        assertThatThrownBy(() -> adapter.verify("response-token"))
                .isInstanceOf(CaptchaProviderUnavailableException.class);
        mockServer.verify();
    }

    @Test
    void verifyThrowsOnMalformedJsonWithSuccessStatus(CapturedOutput output) {
        mockServer.expect(requestTo(SITEVERIFY_URL))
                .andRespond(withSuccess("{not-valid-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.verify("response-token"))
                .isInstanceOf(CaptchaProviderUnavailableException.class);

        String logs = output.getOut() + output.getErr();
        assertThat(logs).contains("captcha provider unavailable");
        assertThat(logs).doesNotContain("invalid-input-response");
        mockServer.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 429})
    void verifyThrowsOnProviderClientErrorStatus(int status) {
        mockServer.expect(requestTo(SITEVERIFY_URL))
                .andRespond(withStatus(HttpStatus.valueOf(status)));

        assertThatThrownBy(() -> adapter.verify("response-token"))
                .isInstanceOf(CaptchaProviderUnavailableException.class);
        mockServer.verify();
    }

    @Test
    void verifyThrowsOnUnknownHostException(CapturedOutput output) {
        GoogleRecaptchaVerificationAdapter failingAdapter = adapterWithRequestFailure(
                new ResourceAccessException("I/O error", new UnknownHostException("www.google.com"))
        );

        assertThatThrownBy(() -> failingAdapter.verify("response-token"))
                .isInstanceOf(CaptchaProviderUnavailableException.class);

        String logs = output.getOut() + output.getErr();
        assertThat(logs).contains("captcha provider unavailable");
        assertThat(logs).contains("siteverify_dns_failed");
        assertThat(logs).doesNotContain("response-token");
        assertThat(logs).doesNotContain("test-secret");
    }

    @Test
    void verifyThrowsOnConnectionReset(CapturedOutput output) {
        GoogleRecaptchaVerificationAdapter failingAdapter = adapterWithRequestFailure(
                new ResourceAccessException("Connection reset", new SocketException("Connection reset"))
        );

        assertThatThrownBy(() -> failingAdapter.verify("response-token"))
                .isInstanceOf(CaptchaProviderUnavailableException.class);

        String logs = output.getOut() + output.getErr();
        assertThat(logs).contains("captcha provider unavailable");
        assertThat(logs).contains("siteverify_connection_failed");
        assertThat(logs).doesNotContain("response-token");
    }

    @Test
    void verifyThrowsOnSslHandshakeFailure(CapturedOutput output) {
        GoogleRecaptchaVerificationAdapter failingAdapter = adapterWithRequestFailure(
                new ResourceAccessException("SSL handshake failed", new SSLHandshakeException("PKIX path building failed"))
        );

        assertThatThrownBy(() -> failingAdapter.verify("response-token"))
                .isInstanceOf(CaptchaProviderUnavailableException.class);

        String logs = output.getOut() + output.getErr();
        assertThat(logs).contains("captcha provider unavailable");
        assertThat(logs).contains("siteverify_ssl_failed");
        assertThat(logs).doesNotContain("response-token");
    }

    @Test
    void verifyThrowsOnResourceAccessExceptionWithoutSpecificCause(CapturedOutput output) {
        GoogleRecaptchaVerificationAdapter failingAdapter = adapterWithRequestFailure(
                new ResourceAccessException("I/O error on POST request")
        );

        assertThatThrownBy(() -> failingAdapter.verify("response-token"))
                .isInstanceOf(CaptchaProviderUnavailableException.class);

        String logs = output.getOut() + output.getErr();
        assertThat(logs).contains("captcha provider unavailable");
        assertThat(logs).contains("siteverify_transport_failed");
        assertThat(logs).doesNotContain("response-token");
    }

    @Test
    void verifyThrowsOnReadTimeout(CapturedOutput output) {
        GoogleRecaptchaVerificationAdapter timeoutAdapter = adapterWithRequestFailure(
                new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out"))
        );

        assertThatThrownBy(() -> timeoutAdapter.verify("response-token"))
                .isInstanceOf(CaptchaProviderUnavailableException.class);

        String logs = output.getOut() + output.getErr();
        assertThat(logs).contains("captcha provider unavailable");
        assertThat(logs).contains("exception=ResourceAccessException");
        assertThat(logs).doesNotContain("response-token");
    }

    @Test
    void verifyThrowsOnConnectTimeout(CapturedOutput output) {
        GoogleRecaptchaVerificationAdapter timeoutAdapter = adapterWithRequestFailure(
                new ResourceAccessException("Connect timed out", new SocketTimeoutException("Connect timed out"))
        );

        assertThatThrownBy(() -> timeoutAdapter.verify("response-token"))
                .isInstanceOf(CaptchaProviderUnavailableException.class);

        String logs = output.getOut() + output.getErr();
        assertThat(logs).contains("captcha provider unavailable");
        assertThat(logs).doesNotContain("response-token");
    }

    @Test
    void verifyDoesNotLogCaptchaToken(CapturedOutput output) {
        mockServer.expect(requestTo(SITEVERIFY_URL))
                .andRespond(withSuccess(validSiteverifyJson("localhost", "2026-08-05T09:59:00Z"), MediaType.APPLICATION_JSON));

        String token = "super-secret-captcha-response-token";
        adapter.verify(token);

        assertThat(output.getOut()).doesNotContain(token);
        assertThat(output.getErr()).doesNotContain(token);
        mockServer.verify();
    }

    @Test
    void verifyDoesNotLogSecretKeyOnFailure(CapturedOutput output) {
        mockServer.expect(requestTo(SITEVERIFY_URL))
                .andRespond(withServerError());

        assertThatThrownBy(() -> adapter.verify("response-token"))
                .isInstanceOf(CaptchaProviderUnavailableException.class);

        String logs = output.getOut() + output.getErr();
        assertThat(logs).doesNotContain("test-secret");
        assertThat(logs).doesNotContain("response-token");
        mockServer.verify();
    }

    @Test
    void recaptchaRestClientFactoryAppliesConfiguredTimeouts() {
        CaptchaProperties timeoutProperties = new CaptchaProperties(
                true,
                1500,
                2500,
                new CaptchaProperties.Recaptcha("test-secret", "localhost,127.0.0.1", 120)
        );

        RestClient restClient = RecaptchaRestClientFactory.create(timeoutProperties);

        assertThat(restClient).isNotNull();
    }

    private GoogleRecaptchaVerificationAdapter adapterWithRequestFailure(RuntimeException failure) {
        RestClient failingRestClient = RestClient.builder()
                .requestFactory((uri, httpMethod) -> {
                    throw failure;
                })
                .build();
        return new GoogleRecaptchaVerificationAdapter(
                captchaProperties,
                new RecaptchaVerificationEvaluator(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                failingRestClient
        );
    }

    private static String validSiteverifyJson(String hostname, String challengeTs) {
        return """
                {
                  "success": true,
                  "challenge_ts": "%s",
                  "hostname": "%s"
                }
                """.formatted(challengeTs, hostname);
    }
}
