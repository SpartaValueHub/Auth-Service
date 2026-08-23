package com.sparta.auth_service.application.service;

import com.sparta.auth_service.application.exception.AuthNotFoundException;
import com.sparta.auth_service.application.port.in.dto.GetMemberJoinedAtResultDto;
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
class GetMemberJoinedAtServiceTest {

    private static final String MEMBER_UUID = "550e8400-e29b-41d4-a716-446655440000";
    private static final Instant JOINED_AT = Instant.parse("2026-08-04T08:00:00Z");

    @Mock
    private AuthRepositoryPort authRepositoryPort;

    @InjectMocks
    private GetMemberJoinedAtService getMemberJoinedAtService;

    @Test
    void getMemberJoinedAt_returnsJoinedAtForActiveAccount() {
        when(authRepositoryPort.findByAuthUuid(MEMBER_UUID)).thenReturn(Optional.of(sampleAuth(MemberStatus.ACTIVE)));

        GetMemberJoinedAtResultDto result = getMemberJoinedAtService.getMemberJoinedAt(MEMBER_UUID);

        assertThat(result.getMemberUuid()).isEqualTo(MEMBER_UUID);
        assertThat(result.getJoinedAt()).isEqualTo(JOINED_AT);
    }

    @Test
    void getMemberJoinedAt_trimsMemberUuid() {
        when(authRepositoryPort.findByAuthUuid(MEMBER_UUID)).thenReturn(Optional.of(sampleAuth(MemberStatus.ACTIVE)));

        GetMemberJoinedAtResultDto result = getMemberJoinedAtService.getMemberJoinedAt("  " + MEMBER_UUID + "  ");

        assertThat(result.getMemberUuid()).isEqualTo(MEMBER_UUID);
    }

    @Test
    void getMemberJoinedAt_throwsWhenNotFound() {
        when(authRepositoryPort.findByAuthUuid(MEMBER_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getMemberJoinedAtService.getMemberJoinedAt(MEMBER_UUID))
                .isInstanceOf(AuthNotFoundException.class)
                .extracting(ex -> ((AuthNotFoundException) ex).getCode())
                .isEqualTo("AUTH_NOT_FOUND");
    }

    @Test
    void getMemberJoinedAt_throwsWhenWithdrawn() {
        when(authRepositoryPort.findByAuthUuid(MEMBER_UUID)).thenReturn(Optional.of(sampleAuth(MemberStatus.WITHDRAWN)));

        assertThatThrownBy(() -> getMemberJoinedAtService.getMemberJoinedAt(MEMBER_UUID))
                .isInstanceOf(AuthNotFoundException.class)
                .extracting(ex -> ((AuthNotFoundException) ex).getCode())
                .isEqualTo("AUTH_NOT_FOUND");
    }

    @Test
    void getMemberJoinedAt_throwsWhenSuspended() {
        when(authRepositoryPort.findByAuthUuid(MEMBER_UUID)).thenReturn(Optional.of(sampleAuth(MemberStatus.SUSPENDED)));

        assertThatThrownBy(() -> getMemberJoinedAtService.getMemberJoinedAt(MEMBER_UUID))
                .isInstanceOf(AuthNotFoundException.class)
                .extracting(ex -> ((AuthNotFoundException) ex).getCode())
                .isEqualTo("AUTH_NOT_FOUND");
    }

    @Test
    void getMemberJoinedAt_throwsWhenDormant() {
        when(authRepositoryPort.findByAuthUuid(MEMBER_UUID)).thenReturn(Optional.of(sampleAuth(MemberStatus.DORMANT)));

        assertThatThrownBy(() -> getMemberJoinedAtService.getMemberJoinedAt(MEMBER_UUID))
                .isInstanceOf(AuthNotFoundException.class)
                .extracting(ex -> ((AuthNotFoundException) ex).getCode())
                .isEqualTo("AUTH_NOT_FOUND");
    }

    @Test
    void getMemberJoinedAt_throwsWhenMemberUuidBlank() {
        assertThatThrownBy(() -> getMemberJoinedAtService.getMemberJoinedAt("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memberUuid");
    }

    private static AuthDomain sampleAuth(MemberStatus memberStatus) {
        return AuthDomain.reconstitute(
                MEMBER_UUID,
                "user01",
                "홍길동",
                LocalDate.of(1990, 1, 1),
                "01012345678",
                Gender.MALE,
                "user@example.com",
                "encoded-password-hash",
                JOINED_AT,
                memberStatus,
                JOINED_AT,
                JOINED_AT
        );
    }
}
