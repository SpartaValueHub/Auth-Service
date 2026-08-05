package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.application.exception.SecurityStoreUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAccessTokenBlacklistAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisAccessTokenBlacklistAdapter adapter;

    @Test
    void blacklist_skipsRedisWriteWhenTtlIsZero() {
        adapter.blacklist("jti-001", 0L);

        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void blacklist_writesWhenTtlIsPositive() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        adapter.blacklist("jti-001", 60L);

        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void blacklist_throwsWhenRedisFails() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("down"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        assertThatThrownBy(() -> adapter.blacklist("jti-001", 60L))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }

    @Test
    void isBlacklisted_throwsWhenRedisFails() {
        when(stringRedisTemplate.hasKey("auth:blacklist:access:jti-001"))
                .thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> adapter.isBlacklisted("jti-001"))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }
}
