package com.sparta.auth_service.adaptor.out.security;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsNonPositiveAccessTokenMinutes() {
        JwtProperties properties = new JwtProperties(null, null, 0, 14, 120);

        assertThat(validator.validate(properties)).isNotEmpty();
    }

    @Test
    void rejectsNonPositiveRefreshTokenDays() {
        JwtProperties properties = new JwtProperties(null, null, 15, 0, 120);

        assertThat(validator.validate(properties)).isNotEmpty();
    }

    @Test
    void rejectsNonPositiveSignupCompletionTokenSeconds() {
        JwtProperties properties = new JwtProperties(null, null, 15, 14, 0);

        assertThat(validator.validate(properties)).isNotEmpty();
    }

    @Test
    void acceptsPositiveValues() {
        assertThat(validator.validate(validProperties())).isEmpty();
    }

    private JwtProperties validProperties() {
        return new JwtProperties(null, null, 15, 14, 120);
    }
}
