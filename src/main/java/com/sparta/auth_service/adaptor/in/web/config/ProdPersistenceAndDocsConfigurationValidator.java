package com.sparta.auth_service.adaptor.in.web.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * prod 프로필 JPA·Springdoc fail-closed 검증.
 * 환경 변수 override 포함 최종 Environment 값을 검사한다.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
class ProdPersistenceAndDocsConfigurationValidator {

    private static final String DDL_AUTO_PROPERTY = "spring.jpa.hibernate.ddl-auto";
    private static final String SHOW_SQL_PROPERTY = "spring.jpa.show-sql";
    private static final String FORMAT_SQL_PROPERTY = "spring.jpa.properties.hibernate.format_sql";
    private static final String API_DOCS_ENABLED_PROPERTY = "springdoc.api-docs.enabled";
    private static final String SWAGGER_UI_ENABLED_PROPERTY = "springdoc.swagger-ui.enabled";

    private final Environment environment;

    @PostConstruct
    void validateProdPersistenceAndDocsConfiguration() {
        validateDdlAuto();
        validateShowSql();
        validateFormatSql();
        validateSpringdocDisabled(API_DOCS_ENABLED_PROPERTY, "springdoc.api-docs.enabled");
        validateSpringdocDisabled(SWAGGER_UI_ENABLED_PROPERTY, "springdoc.swagger-ui.enabled");
    }

    private void validateDdlAuto() {
        String ddlAuto = environment.getProperty(DDL_AUTO_PROPERTY);
        if (!StringUtils.hasText(ddlAuto)) {
            throw new IllegalStateException(
                    "prod profile requires spring.jpa.hibernate.ddl-auto=validate");
        }
        if (!"validate".equalsIgnoreCase(ddlAuto.trim())) {
            throw new IllegalStateException(
                    "prod profile requires spring.jpa.hibernate.ddl-auto=validate");
        }
    }

    private void validateShowSql() {
        Boolean showSql = environment.getProperty(SHOW_SQL_PROPERTY, Boolean.class);
        if (showSql == null) {
            throw new IllegalStateException("prod profile requires spring.jpa.show-sql=false");
        }
        if (showSql) {
            throw new IllegalStateException("prod profile requires spring.jpa.show-sql=false");
        }
    }

    private void validateFormatSql() {
        Boolean formatSql = environment.getProperty(FORMAT_SQL_PROPERTY, Boolean.class);
        if (formatSql == null) {
            throw new IllegalStateException(
                    "prod profile requires spring.jpa.properties.hibernate.format_sql=false");
        }
        if (formatSql) {
            throw new IllegalStateException(
                    "prod profile requires spring.jpa.properties.hibernate.format_sql=false");
        }
    }

    private void validateSpringdocDisabled(String propertyName, String displayName) {
        Boolean enabled = environment.getProperty(propertyName, Boolean.class);
        if (enabled == null) {
            throw new IllegalStateException("prod profile requires " + displayName + "=false");
        }
        if (enabled) {
            throw new IllegalStateException("prod profile requires " + displayName + "=false");
        }
    }
}
