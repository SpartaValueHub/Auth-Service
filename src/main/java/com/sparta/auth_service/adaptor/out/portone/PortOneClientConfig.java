package com.sparta.auth_service.adaptor.out.portone;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
class PortOneClientConfig {

    @Bean
    @ConditionalOnProperty(name = "portone.api-secret")
    RestClient portOneRestClient(PortOneProperties properties) {
        if (!StringUtils.hasText(properties.getApiSecret())) {
            throw new IllegalStateException("portone.api-secret must be configured");
        }
        return PortOneRestClientFactory.create(properties);
    }
}
