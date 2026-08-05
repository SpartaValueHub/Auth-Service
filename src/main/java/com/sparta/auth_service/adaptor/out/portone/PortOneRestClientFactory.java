package com.sparta.auth_service.adaptor.out.portone;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** PortOne API용 RestClient — connect/read 타임아웃 적용 */
final class PortOneRestClientFactory {

    private PortOneRestClientFactory() {
    }

    static RestClient create(PortOneProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMillis()));
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "PortOne " + properties.getApiSecret())
                .requestFactory(requestFactory)
                .build();
    }
}
