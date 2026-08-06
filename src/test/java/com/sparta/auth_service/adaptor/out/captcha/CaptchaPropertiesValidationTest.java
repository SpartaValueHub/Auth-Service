package com.sparta.auth_service.adaptor.out.captcha;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaPropertiesValidationTest {

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
    @ValueSource(ints = {0, -1})
    void rejectsInvalidChallengeMaxAgeSeconds(int value) {
        CaptchaProperties properties = new CaptchaProperties(
                true,
                2000,
                3000,
                new CaptchaProperties.Recaptcha("", "localhost", value)
        );

        assertThat(VALIDATOR.validate(properties)).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidConnectTimeoutMillis(int value) {
        CaptchaProperties properties = new CaptchaProperties(
                true,
                value,
                3000,
                new CaptchaProperties.Recaptcha("", "localhost", 120)
        );

        assertThat(VALIDATOR.validate(properties)).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidReadTimeoutMillis(int value) {
        CaptchaProperties properties = new CaptchaProperties(
                true,
                2000,
                value,
                new CaptchaProperties.Recaptcha("", "localhost", 120)
        );

        assertThat(VALIDATOR.validate(properties)).isNotEmpty();
    }

    @Test
    void contextFailsOnInvalidBoundChallengeMaxAgeSeconds() {
        contextRunner
                .withPropertyValues("captcha.recaptcha.challenge-max-age-seconds=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void contextFailsOnInvalidBoundConnectTimeoutMillis() {
        contextRunner
                .withPropertyValues("captcha.connect-timeout-millis=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void contextFailsOnInvalidBoundReadTimeoutMillis() {
        contextRunner
                .withPropertyValues("captcha.read-timeout-millis=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(CaptchaProperties.class)
    static class TestConfig {
    }
}
