package com.sparta.auth_service.adaptor.in.web.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimitPropertiesValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    ValidationAutoConfiguration.class,
                    ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void acceptsValidDefaults() {
        contextRunner.run(context -> assertThat(context).hasNotFailed());
    }

    @ParameterizedTest
    @CsvSource({
            "maxAttempts, 0",
            "windowSeconds, 0",
            "blockSeconds, 0",
            "maxAttempts, -1",
            "windowSeconds, -5",
            "blockSeconds, -10"
    })
    void rejectsInvalidFieldValues(String field, int value) {
        LoginRateLimitProperties properties = new LoginRateLimitProperties();
        switch (field) {
            case "maxAttempts" -> properties.setMaxAttempts(value);
            case "windowSeconds" -> properties.setWindowSeconds(value);
            case "blockSeconds" -> properties.setBlockSeconds(value);
            default -> throw new IllegalArgumentException("unknown field: " + field);
        }

        assertThat(VALIDATOR.validate(properties)).isNotEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "auth.login-rate-limit.max-attempts=0",
            "auth.login-rate-limit.window-seconds=0",
            "auth.login-rate-limit.block-seconds=0"
    })
    void contextFailsOnInvalidBoundProperties(String invalidProperty) {
        contextRunner
                .withPropertyValues(invalidProperty)
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(LoginRateLimitProperties.class)
    static class TestConfig {
    }
}
