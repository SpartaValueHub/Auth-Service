package com.sparta.auth_service.adaptor.out.mysql;

import com.sparta.auth_service.application.exception.DuplicateResourceException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AuthDataIntegrityViolationMapperTest {

    @Test
    void mapsKnownConstraintNames() {
        assertThat(map("uk_auth_login_id").get().getCode()).isEqualTo("AUTH_DUPLICATE_LOGIN_ID");
        assertThat(map("uk_auth_email").get().getCode()).isEqualTo("AUTH_DUPLICATE_EMAIL");
        assertThat(map("uk_auth_phone_number").get().getCode()).isEqualTo("AUTH_DUPLICATE_PHONE");
    }

    @Test
    void mapsUnknownDuplicateEntryToGenericCode() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "Duplicate entry 'x' for key 'uk_unknown'"
        );

        assertThat(AuthDataIntegrityViolationMapper.isNonDuplicateIntegrityViolation(ex)).isFalse();
        assertThat(AuthDataIntegrityViolationMapper.isDuplicateEntryViolation(ex)).isTrue();
        assertThat(AuthDataIntegrityViolationMapper.mapDuplicate(ex)).isEmpty();
    }

    @Test
    void doesNotTreatNotNullAsDuplicate() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Column 'email' cannot be null");

        assertThat(AuthDataIntegrityViolationMapper.isNonDuplicateIntegrityViolation(ex)).isTrue();
        assertThat(AuthDataIntegrityViolationMapper.mapDuplicate(ex)).isEmpty();
    }

    @Test
    void doesNotTreatForeignKeyAsDuplicate() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "Cannot add or update a child row: a foreign key constraint fails"
        );

        assertThat(AuthDataIntegrityViolationMapper.isNonDuplicateIntegrityViolation(ex)).isTrue();
        assertThat(AuthDataIntegrityViolationMapper.mapDuplicate(ex)).isEmpty();
    }

    @Test
    void prefersHibernateConstraintNameOverMessage() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "wrapper",
                new ConstraintViolationException(
                        "nested",
                        new SQLException("Duplicate entry", "23000", 1062),
                        "uk_auth_login_id"
                )
        );

        assertThat(AuthDataIntegrityViolationMapper.mapDuplicate(ex))
                .map(DuplicateResourceException::getCode)
                .contains("AUTH_DUPLICATE_LOGIN_ID");
    }

    @Test
    void messageFallbackHandlesMysqlDuplicateEntryFormat() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "Duplicate entry 'a@b.com' for key 'uk_auth_email'"
        );

        assertThat(AuthDataIntegrityViolationMapper.mapDuplicate(ex))
                .map(DuplicateResourceException::getCode)
                .contains("AUTH_DUPLICATE_EMAIL");
    }

    @Test
    void returnsEmptyForNullMessagesWithoutConstraintName() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException((String) null);

        assertThat(AuthDataIntegrityViolationMapper.mapDuplicate(ex)).isEmpty();
        assertThat(AuthDataIntegrityViolationMapper.isDuplicateEntryViolation(ex)).isFalse();
    }

    private Optional<DuplicateResourceException> map(String constraintName) {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "wrapper",
                new ConstraintViolationException(
                        "nested",
                        new SQLException("Duplicate entry", "23000", 1062),
                        constraintName
                )
        );
        return AuthDataIntegrityViolationMapper.mapDuplicate(ex);
    }
}
