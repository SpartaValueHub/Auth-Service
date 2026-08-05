package com.sparta.auth_service.adaptor.out.mysql;

import com.sparta.auth_service.adaptor.out.mysql.entity.AuthEntity;
import com.sparta.auth_service.adaptor.out.mysql.mapper.AuthEntityMapper;
import com.sparta.auth_service.adaptor.out.mysql.repository.AuthJpaRepository;
import com.sparta.auth_service.application.exception.DuplicateResourceException;
import com.sparta.auth_service.domain.enums.Gender;
import com.sparta.auth_service.domain.enums.MemberStatus;
import com.sparta.auth_service.domain.model.AuthDomain;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRepositoryAdapterTest {

    @Mock
    private AuthJpaRepository authJpaRepository;

    @Mock
    private AuthEntityMapper authEntityMapper;

    @InjectMocks
    private AuthRepositoryAdapter authRepositoryAdapter;

    @Test
    void save_convertsEmailDuplicateFromFlushTo409() {
        AuthDomain domain = sampleDomain();
        when(authJpaRepository.findByAuthUuid(domain.getAuthUuid())).thenReturn(Optional.empty());
        when(authEntityMapper.toEntity(domain)).thenReturn(AuthEntity.builder().build());
        when(authJpaRepository.saveAndFlush(any(AuthEntity.class)))
                .thenThrow(emailDuplicate("Duplicate entry 'user@example.com' for key 'uk_auth_email'"));

        assertThatThrownBy(() -> authRepositoryAdapter.save(domain))
                .isInstanceOf(DuplicateResourceException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_DUPLICATE_EMAIL");
    }

    @Test
    void save_convertsPhoneDuplicateFromConstraintName() {
        AuthDomain domain = sampleDomain();
        when(authJpaRepository.findByAuthUuid(domain.getAuthUuid())).thenReturn(Optional.empty());
        when(authEntityMapper.toEntity(domain)).thenReturn(AuthEntity.builder().build());
        when(authJpaRepository.saveAndFlush(any(AuthEntity.class)))
                .thenThrow(hibernateDuplicate("uk_auth_phone_number"));

        assertThatThrownBy(() -> authRepositoryAdapter.save(domain))
                .isInstanceOf(DuplicateResourceException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_DUPLICATE_PHONE");
    }

    @Test
    void save_convertsLoginIdDuplicateFromConstraintName() {
        AuthDomain domain = sampleDomain();
        when(authJpaRepository.findByAuthUuid(domain.getAuthUuid())).thenReturn(Optional.empty());
        when(authEntityMapper.toEntity(domain)).thenReturn(AuthEntity.builder().build());
        when(authJpaRepository.saveAndFlush(any(AuthEntity.class)))
                .thenThrow(hibernateDuplicate("uk_auth_login_id"));

        assertThatThrownBy(() -> authRepositoryAdapter.save(domain))
                .isInstanceOf(DuplicateResourceException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_DUPLICATE_LOGIN_ID");
    }

    @Test
    void save_convertsUnknownDuplicateEntryToGeneric409() {
        AuthDomain domain = sampleDomain();
        when(authJpaRepository.findByAuthUuid(domain.getAuthUuid())).thenReturn(Optional.empty());
        when(authEntityMapper.toEntity(domain)).thenReturn(AuthEntity.builder().build());
        when(authJpaRepository.saveAndFlush(any(AuthEntity.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry 'value' for key 'uk_unknown_field'"
                ));

        assertThatThrownBy(() -> authRepositoryAdapter.save(domain))
                .isInstanceOf(DuplicateResourceException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_DUPLICATE");
    }

    @Test
    void save_rethrowsNotNullViolationWithout409Mapping() {
        AuthDomain domain = sampleDomain();
        when(authJpaRepository.findByAuthUuid(domain.getAuthUuid())).thenReturn(Optional.empty());
        when(authEntityMapper.toEntity(domain)).thenReturn(AuthEntity.builder().build());
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "Column 'email' cannot be null"
        );
        when(authJpaRepository.saveAndFlush(any(AuthEntity.class))).thenThrow(ex);

        assertThatThrownBy(() -> authRepositoryAdapter.save(domain))
                .isSameAs(ex);
    }

    @Test
    void save_rethrowsForeignKeyViolationWithout409Mapping() {
        AuthDomain domain = sampleDomain();
        when(authJpaRepository.findByAuthUuid(domain.getAuthUuid())).thenReturn(Optional.empty());
        when(authEntityMapper.toEntity(domain)).thenReturn(AuthEntity.builder().build());
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "Cannot add or update a child row: a foreign key constraint fails"
        );
        when(authJpaRepository.saveAndFlush(any(AuthEntity.class))).thenThrow(ex);

        assertThatThrownBy(() -> authRepositoryAdapter.save(domain))
                .isSameAs(ex);
    }

    @Test
    void save_mapsNestedHibernateConstraintNameWhenOuterMessageIsNull() {
        AuthDomain domain = sampleDomain();
        when(authJpaRepository.findByAuthUuid(domain.getAuthUuid())).thenReturn(Optional.empty());
        when(authEntityMapper.toEntity(domain)).thenReturn(AuthEntity.builder().build());

        ConstraintViolationException hibernateEx = new ConstraintViolationException(
                "constraint violation",
                new SQLException("Duplicate entry", "23000", 1062),
                "uk_auth_email"
        );
        DataIntegrityViolationException ex = new DataIntegrityViolationException("flush failed", hibernateEx);
        when(authJpaRepository.saveAndFlush(any(AuthEntity.class))).thenThrow(ex);

        assertThatThrownBy(() -> authRepositoryAdapter.save(domain))
                .isInstanceOf(DuplicateResourceException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_DUPLICATE_EMAIL");
    }

    @Test
    void save_mapsMessageFallbackWhenConstraintNameMissing() {
        AuthDomain domain = sampleDomain();
        when(authJpaRepository.findByAuthUuid(domain.getAuthUuid())).thenReturn(Optional.empty());
        when(authEntityMapper.toEntity(domain)).thenReturn(AuthEntity.builder().build());
        when(authJpaRepository.saveAndFlush(any(AuthEntity.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "could not execute statement",
                        new ConstraintViolationException(
                                "could not execute statement",
                                new SQLException("Duplicate entry '01012345678' for key 'auth.phone_number'", "23000", 1062),
                                null
                        )
                ));

        assertThatThrownBy(() -> authRepositoryAdapter.save(domain))
                .isInstanceOf(DuplicateResourceException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_DUPLICATE_PHONE");
    }

    private DataIntegrityViolationException emailDuplicate(String message) {
        return new DataIntegrityViolationException(message);
    }

    private DataIntegrityViolationException hibernateDuplicate(String constraintName) {
        return new DataIntegrityViolationException(
                "could not execute statement",
                new ConstraintViolationException(
                        "could not execute statement",
                        new SQLException("Duplicate entry", "23000", 1062),
                        constraintName
                )
        );
    }

    private AuthDomain sampleDomain() {
        return AuthDomain.reconstitute(
                "uuid-001",
                "user01",
                "홍길동",
                LocalDate.of(1990, 1, 1),
                "01012345678",
                Gender.MALE,
                "user@example.com",
                "hash",
                Instant.parse("2024-03-01T00:00:00Z"),
                MemberStatus.ACTIVE,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-06-01T00:00:00Z")
        );
    }
}
