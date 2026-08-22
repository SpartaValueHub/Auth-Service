package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.adaptor.out.security.JwtProperties;
import com.sparta.auth_service.application.port.out.AccessTokenBlacklistPort;
import com.sparta.auth_service.application.port.out.ActiveAccessTokenPort;
import com.sparta.auth_service.application.port.out.RefreshTokenPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisSessionInvalidationAdapterTest {

    @Mock
    private ActiveAccessTokenPort activeAccessTokenPort;
    @Mock
    private AccessTokenBlacklistPort accessTokenBlacklistPort;
    @Mock
    private RefreshTokenPort refreshTokenPort;
    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RedisSessionInvalidationAdapter adapter;

    @Test
    void revokeAllSessions_blacklistsActiveJtiThenDeletesKeys() {
        when(activeAccessTokenPort.find("uuid-001")).thenReturn(Optional.of("access-jti"));
        when(jwtProperties.getAccessTokenMinutes()).thenReturn(30L);

        adapter.revokeAllSessions("uuid-001");

        verify(accessTokenBlacklistPort).blacklist(eq("access-jti"), eq(1800L));
        verify(activeAccessTokenPort).delete("uuid-001");
        verify(refreshTokenPort).delete("uuid-001");
    }

    @Test
    void revokeAllSessions_skipsBlacklistWhenNoActiveJti() {
        when(activeAccessTokenPort.find("uuid-001")).thenReturn(Optional.empty());

        adapter.revokeAllSessions("uuid-001");

        verify(accessTokenBlacklistPort, never()).blacklist(any(), any(Long.class));
        verify(activeAccessTokenPort).delete("uuid-001");
        verify(refreshTokenPort).delete("uuid-001");
    }
}
