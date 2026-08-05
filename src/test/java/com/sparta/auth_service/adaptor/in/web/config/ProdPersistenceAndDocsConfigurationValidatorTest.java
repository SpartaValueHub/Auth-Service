package com.sparta.auth_service.adaptor.in.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ProdPersistenceAndDocsConfigurationValidatorTest {

    private static final String[] VALID_PROD_PROPERTIES = {
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.jpa.show-sql=false",
            "spring.jpa.properties.hibernate.format_sql=false",
            "springdoc.api-docs.enabled=false",
            "springdoc.swagger-ui.enabled=false"
    };

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ProdPersistenceAndDocsConfigurationValidator.class)
            .withPropertyValues("spring.profiles.active=prod");

    @Test
    void prodStartsWithValidPersistenceAndDocsConfiguration() {
        contextRunner
                .withPropertyValues(VALID_PROD_PROPERTIES)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void prodFailsWhenDdlAutoMissing() {
        contextRunner
                .withPropertyValues(
                        "spring.jpa.show-sql=false",
                        "spring.jpa.properties.hibernate.format_sql=false",
                        "springdoc.api-docs.enabled=false",
                        "springdoc.swagger-ui.enabled=false"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenDdlAutoIsUpdate() {
        contextRunner
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=update",
                        "spring.jpa.show-sql=false",
                        "spring.jpa.properties.hibernate.format_sql=false",
                        "springdoc.api-docs.enabled=false",
                        "springdoc.swagger-ui.enabled=false"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenDdlAutoIsCreate() {
        contextRunner
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=create",
                        "spring.jpa.show-sql=false",
                        "spring.jpa.properties.hibernate.format_sql=false",
                        "springdoc.api-docs.enabled=false",
                        "springdoc.swagger-ui.enabled=false"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenDdlAutoIsCreateDrop() {
        contextRunner
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=create-drop",
                        "spring.jpa.show-sql=false",
                        "spring.jpa.properties.hibernate.format_sql=false",
                        "springdoc.api-docs.enabled=false",
                        "springdoc.swagger-ui.enabled=false"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenDdlAutoIsNone() {
        contextRunner
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=none",
                        "spring.jpa.show-sql=false",
                        "spring.jpa.properties.hibernate.format_sql=false",
                        "springdoc.api-docs.enabled=false",
                        "springdoc.swagger-ui.enabled=false"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenShowSqlTrue() {
        contextRunner
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.jpa.show-sql=true",
                        "spring.jpa.properties.hibernate.format_sql=false",
                        "springdoc.api-docs.enabled=false",
                        "springdoc.swagger-ui.enabled=false"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenShowSqlMissing() {
        contextRunner
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.jpa.properties.hibernate.format_sql=false",
                        "springdoc.api-docs.enabled=false",
                        "springdoc.swagger-ui.enabled=false"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenFormatSqlTrue() {
        contextRunner
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.jpa.show-sql=false",
                        "spring.jpa.properties.hibernate.format_sql=true",
                        "springdoc.api-docs.enabled=false",
                        "springdoc.swagger-ui.enabled=false"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenFormatSqlMissing() {
        contextRunner
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.jpa.show-sql=false",
                        "springdoc.api-docs.enabled=false",
                        "springdoc.swagger-ui.enabled=false"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenApiDocsEnabled() {
        contextRunner
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.jpa.show-sql=false",
                        "spring.jpa.properties.hibernate.format_sql=false",
                        "springdoc.api-docs.enabled=true",
                        "springdoc.swagger-ui.enabled=false"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenSwaggerUiEnabled() {
        contextRunner
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.jpa.show-sql=false",
                        "spring.jpa.properties.hibernate.format_sql=false",
                        "springdoc.api-docs.enabled=false",
                        "springdoc.swagger-ui.enabled=true"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenApiDocsEnabledMissing() {
        contextRunner
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.jpa.show-sql=false",
                        "spring.jpa.properties.hibernate.format_sql=false",
                        "springdoc.swagger-ui.enabled=false"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodFailsWhenSwaggerUiEnabledMissing() {
        contextRunner
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.jpa.show-sql=false",
                        "spring.jpa.properties.hibernate.format_sql=false",
                        "springdoc.api-docs.enabled=false"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void nonProdProfileDoesNotLoadProdPersistenceValidator() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "spring.profiles.active=local",
                        "spring.jpa.hibernate.ddl-auto=update",
                        "spring.jpa.show-sql=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(ProdPersistenceAndDocsConfigurationValidator.class));
    }
}
