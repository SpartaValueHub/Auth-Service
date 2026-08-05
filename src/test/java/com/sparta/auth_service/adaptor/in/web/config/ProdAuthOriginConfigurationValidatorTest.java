package com.sparta.auth_service.adaptor.in.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ProdAuthOriginConfigurationValidatorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class, ProdAuthOriginConfigurationValidator.class)
            .withPropertyValues("spring.profiles.active=prod");

    @Test
    void prodStartsWithValidOrigins() {
        contextRunner
                .withPropertyValues(
                        "auth.origin.require-origin=true",
                        "auth.origin.allowed-origins=https://valuehub.example.com"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void prodFailsWhenRequireOriginFalse() {
        contextRunner
                .withPropertyValues(
                        "auth.origin.require-origin=false",
                        "auth.origin.allowed-origins=https://valuehub.example.com"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenAllowedOriginsEmpty() {
        contextRunner
                .withPropertyValues(
                        "auth.origin.require-origin=true",
                        "auth.origin.allowed-origins="
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenAllowedOriginsInvalid() {
        contextRunner
                .withPropertyValues(
                        "auth.origin.require-origin=true",
                        "auth.origin.allowed-origins=https://*.example.com"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenLocalhostAllowed() {
        contextRunner
                .withPropertyValues(
                        "auth.origin.require-origin=true",
                        "auth.origin.allowed-origins=http://localhost:3000"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenLoopbackAllowed() {
        contextRunner
                .withPropertyValues(
                        "auth.origin.require-origin=true",
                        "auth.origin.allowed-origins=http://127.0.0.1:3000"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsOnDuplicateOrigins() {
        contextRunner
                .withPropertyValues(
                        "auth.origin.require-origin=true",
                        "auth.origin.allowed-origins=https://valuehub.example.com,https://valuehub.example.com"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void nonProdProfileDoesNotLoadProdValidator() {
        new ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        ConfigurationPropertiesAutoConfiguration.class))
                .withUserConfiguration(TestConfig.class)
                .withPropertyValues(
                        "spring.profiles.active=local",
                        "auth.origin.require-origin=false",
                        "auth.origin.allowed-origins=http://localhost:3000"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ProdAuthOriginConfigurationValidator.class);
                });
    }

    @Configuration
    @EnableConfigurationProperties(AuthOriginProperties.class)
    static class TestConfig {
    }
}
