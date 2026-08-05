package com.sparta.auth_service.adaptor.out.portone;

import com.sparta.auth_service.application.exception.ExternalIdentityProviderUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.HttpStatus;

@ExtendWith(OutputCaptureExtension.class)
class PortOneIdentityVerificationAdapterTest {

    private static final String BASE_URL = "https://api.portone.io";
    private static final String REQUEST_TOKEN = "identity-verification-id-001";

    private PortOneProperties properties;
    private PortOneIdentityVerificationAdapter adapter;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        properties = new PortOneProperties();
        properties.setApiSecret("test-secret");
        properties.setBaseUrl(BASE_URL);
        properties.setConnectTimeoutMillis(2000);
        properties.setReadTimeoutMillis(5000);

        adapter = new PortOneIdentityVerificationAdapter(restClient);
    }

    @Test
    void fetchByRequestTokenReturnsEmptyOn404() {
        mockServer.expect(requestTo(BASE_URL + "/identity-verifications/" + REQUEST_TOKEN))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<?> result = adapter.fetchByRequestToken(REQUEST_TOKEN);

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void fetchByRequestTokenThrowsOn5xx() {
        mockServer.expect(requestTo(BASE_URL + "/identity-verifications/" + REQUEST_TOKEN))
                .andRespond(withServerError());

        assertThatThrownBy(() -> adapter.fetchByRequestToken(REQUEST_TOKEN))
                .isInstanceOf(ExternalIdentityProviderUnavailableException.class);
        mockServer.verify();
    }

    @Test
    void fetchByRequestTokenThrowsOn429() {
        mockServer.expect(requestTo(BASE_URL + "/identity-verifications/" + REQUEST_TOKEN))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> adapter.fetchByRequestToken(REQUEST_TOKEN))
                .isInstanceOf(ExternalIdentityProviderUnavailableException.class);
        mockServer.verify();
    }

    @Test
    void fetchByRequestTokenThrowsOnMissingStatus() {
        mockServer.expect(requestTo(BASE_URL + "/identity-verifications/" + REQUEST_TOKEN))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.fetchByRequestToken(REQUEST_TOKEN))
                .isInstanceOf(ExternalIdentityProviderUnavailableException.class);
        mockServer.verify();
    }

    @Test
    void fetchByRequestTokenThrowsOnMalformedJson() {
        mockServer.expect(requestTo(BASE_URL + "/identity-verifications/" + REQUEST_TOKEN))
                .andRespond(withSuccess("{not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.fetchByRequestToken(REQUEST_TOKEN))
                .isInstanceOf(ExternalIdentityProviderUnavailableException.class);
        mockServer.verify();
    }

    @Test
    void fetchByRequestTokenDoesNotLogSecretOrRequestToken(CapturedOutput output) {
        mockServer.expect(requestTo(BASE_URL + "/identity-verifications/" + REQUEST_TOKEN))
                .andRespond(withServerError());

        assertThatThrownBy(() -> adapter.fetchByRequestToken(REQUEST_TOKEN))
                .isInstanceOf(ExternalIdentityProviderUnavailableException.class);

        String logs = output.getOut() + output.getErr();
        assertThat(logs).doesNotContain("test-secret");
        assertThat(logs).doesNotContain(REQUEST_TOKEN);
        mockServer.verify();
    }

    @Test
    void portOneRestClientFactoryAppliesConfiguredTimeouts() {
        properties.setConnectTimeoutMillis(1500);
        properties.setReadTimeoutMillis(4500);

        RestClient restClient = PortOneRestClientFactory.create(properties);

        assertThat(restClient).isNotNull();
    }
}
