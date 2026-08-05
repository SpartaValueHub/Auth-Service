package com.sparta.auth_service.adaptor.out.mysql.entity;

import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthEntityTest {

    @Test
    void declaresNamedUniqueConstraintsWithoutColumnUniqueFlag() {
        Table table = AuthEntity.class.getAnnotation(Table.class);

        assertThat(table).isNotNull();
        assertThat(table.uniqueConstraints())
                .extracting(UniqueConstraint::name)
                .containsExactlyInAnyOrder(
                        "uk_auth_auth_uuid",
                        "uk_auth_login_id",
                        "uk_auth_email",
                        "uk_auth_phone_number"
                );
    }
}
