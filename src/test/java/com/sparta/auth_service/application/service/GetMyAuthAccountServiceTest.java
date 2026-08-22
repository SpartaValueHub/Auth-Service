package com.sparta.auth_service.application.service;

import com.sparta.auth_service.application.exception.AuthNotFoundException;
import com.sparta.auth_service.application.port.in.dto.GetMyAuthAccountResultDto;
import com.sparta.auth_service.application.port.out.AuthRepositoryPort;
import com.sparta.auth_service.domain.enums.Gender;
import com.sparta.auth_service.domain.enums.MemberStatus;
import com.sparta.auth_service.domain.model.AuthDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMyAuthAccountServiceTest {

    private static final String AUTH_UUID = "550e8400-e29b-41d4-a716-446655440000";
    private static final Instant JOINED_AT = Instant.parse("2026-08-04T08:00:00Z");

    @Mock
    private AuthRepositoryPort authRepositoryPort;

    @InjectMocks
    private GetMyAuthAccountService getMyAuthAccountService;

    @Test
    void getMyAuthAccount_returnsAccountFieldsWithoutPassword() {
        when(authRepositoryPort.findByAuthUuid(AUTH_UUID)).thenReturn(Optional.of(sampleAuth()));

        GetMyAuthAccountResultDto result = getMyAuthAccountService.getMyAuthAccount(AUTH_UUID);

        assertThat(result.getAuthUuid()).isEqualTo(AUTH_UUID);
        assertThat(result.getLoginId()).isEqualTo("user01");
        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getPhoneNumber()).isEqualTo("01012345678");
        assertThat(result.getJoinedAt()).isEqualTo(JOINED_AT);
    }

    @Test
    void getMyAuthAccount_trimsAuthUuid() {
        when(authRepositoryPort.findByAuthUuid(AUTH_UUID)).thenReturn(Optional.of(sampleAuth()));

        GetMyAuthAccountResultDto result = getMyAuthAccountService.getMyAuthAccount("  " + AUTH_UUID + "  ");

        assertThat(result.getAuthUuid()).isEqualTo(AUTH_UUID);
    }

    @Test
    void getMyAuthAccount_throwsWhenNotFound() {
        when(authRepositoryPort.findByAuthUuid(AUTH_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getMyAuthAccountService.getMyAuthAccount(AUTH_UUID))
                .isInstanceOf(AuthNotFoundException.class)
                .hasMessageContaining("계정을 찾을 수 없습니다.")
                .extracting(ex -> ((AuthNotFoundException) ex).getCode())
                .isEqualTo("AUTH_NOT_FOUND");
    }

    @Test
    void getMyAuthAccount_throwsWhenAuthUuidBlank() {
        assertThatThrownBy(() -> getMyAuthAccountService.getMyAuthAccount("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("authUuid");
    }

    private static AuthDomain sampleAuth() {
        return AuthDomain.reconstitute(
                AUTH_UUID,
                "user01",
                "홍길동",
                LocalDate.of(1990, 1, 1),
                "01012345678",
                Gender.MALE,
                "user@example.com",
                "encoded-password-hash",
                JOINED_AT,
                MemberStatus.ACTIVE,
                JOINED_AT,
                JOINED_AT
        );
    }
}
