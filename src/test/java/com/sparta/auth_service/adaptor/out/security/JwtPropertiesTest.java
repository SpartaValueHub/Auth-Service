package com.sparta.auth_service.adaptor.out.security;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsNonPositiveAccessTokenMinutes() {
        JwtProperties properties = validProperties();
        properties.setAccessTokenMinutes(0);

        assertThat(validator.validate(properties)).isNotEmpty();
    }

    @Test
    void rejectsNonPositiveRefreshTokenDays() {
        JwtProperties properties = validProperties();
        properties.setRefreshTokenDays(0);

        assertThat(validator.validate(properties)).isNotEmpty();
    }

    @Test
    void acceptsPositiveValues() {
        assertThat(validator.validate(validProperties())).isEmpty();
    }

    private JwtProperties validProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setAccessTokenMinutes(15);
        properties.setRefreshTokenDays(14);
        return properties;
    }
}
