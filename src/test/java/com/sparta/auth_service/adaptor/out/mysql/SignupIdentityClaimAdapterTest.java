package com.sparta.auth_service.adaptor.out.mysql;

import com.sparta.auth_service.adaptor.out.mysql.entity.SignupIdentityClaimEntity;
import com.sparta.auth_service.adaptor.out.mysql.repository.SignupIdentityClaimJpaRepository;
import com.sparta.auth_service.application.exception.DuplicateResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupIdentityClaimAdapterTest {

    @Mock SignupIdentityClaimJpaRepository repository;
    @InjectMocks SignupIdentityClaimAdapter adapter;

    @Test
    void claim_savesNormalizedClaim() {
        adapter.claim(" ci-hash ", " auth-uuid ");

        ArgumentCaptor<SignupIdentityClaimEntity> captor =
                ArgumentCaptor.forClass(SignupIdentityClaimEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCiHash()).isEqualTo("ci-hash");
        assertThat(captor.getValue().getAuthUuid()).isEqualTo("auth-uuid");
    }

    @Test
    void claim_mapsUniqueConflictToIdentityDuplicate() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> adapter.claim("ci-hash", "auth-uuid"))
                .isInstanceOf(DuplicateResourceException.class)
                .extracting("code")
                .isEqualTo("AUTH_DUPLICATE_IDENTITY");
    }

    @Test
    void existsByCiHash_delegatesToRepositoryWithTrimmedValue() {
        when(repository.existsByCiHash("ci-hash")).thenReturn(true);

        assertThat(adapter.existsByCiHash(" ci-hash ")).isTrue();
        verify(repository).existsByCiHash("ci-hash");
    }

    @Test
    void releaseByAuthUuid_deletesByTrimmedAuthUuid() {
        adapter.releaseByAuthUuid(" auth-uuid ");

        verify(repository).deleteByAuthUuid("auth-uuid");
    }
}
