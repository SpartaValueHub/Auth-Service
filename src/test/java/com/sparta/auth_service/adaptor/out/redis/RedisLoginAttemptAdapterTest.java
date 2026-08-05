package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.adaptor.in.web.config.LoginAttemptProperties;
import com.sparta.auth_service.application.exception.SecurityStoreUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLoginAttemptAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private LoginAttemptProperties loginAttemptProperties;

    private RedisLoginAttemptAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisLoginAttemptAdapter(stringRedisTemplate, loginAttemptProperties);
    }

    @Test
    void getLockRemainingSecondsReturnsRedisTtl() {
        when(stringRedisTemplate.getExpire("login:lock:user01", TimeUnit.SECONDS)).thenReturn(85L);

        assertThat(adapter.getLockRemainingSeconds("user01")).isEqualTo(85L);
    }

    @Test
    void getLockRemainingSecondsReturnsZeroWhenKeyMissing() {
        when(stringRedisTemplate.getExpire("login:lock:user01", TimeUnit.SECONDS)).thenReturn(-2L);

        assertThat(adapter.getLockRemainingSeconds("user01")).isZero();
    }

    @Test
    void getFailCountThrowsWhenRedisFails() {
        when(stringRedisTemplate.opsForValue()).thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> adapter.getFailCount("user01"))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }

    @Test
    void getFailCountThrowsOnCorruptedValue() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:fail:user01")).thenReturn("not-a-number");

        assertThatThrownBy(() -> adapter.getFailCount("user01"))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }

    @Test
    void incrementFailCountThrowsWhenRedisFails() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("login:fail:user01")).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> adapter.incrementFailCount("user01"))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }

    @Test
    void isLockedThrowsWhenRedisFails() {
        when(stringRedisTemplate.hasKey("login:lock:user01")).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> adapter.isLocked("user01"))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }

    @Test
    void lockThrowsWhenRedisFails() {
        when(loginAttemptProperties.getLockDurationMinutes()).thenReturn(1);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("down"))
                .when(valueOperations).set(anyString(), anyString(), any(java.time.Duration.class));

        assertThatThrownBy(() -> adapter.lock("user01"))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }

    @Test
    void resetThrowsWhenRedisFails() {
        when(stringRedisTemplate.delete(anyString())).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> adapter.reset("user01"))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }

    @Test
    void incrementFailCountSetsTtlOnFirstFailure() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("login:fail:user01")).thenReturn(1L);
        when(loginAttemptProperties.getFailCountWindowMinutes()).thenReturn(10);

        int count = adapter.incrementFailCount("user01");

        assertThat(count).isEqualTo(1);
        verify(stringRedisTemplate).expire(eq("login:fail:user01"), any(java.time.Duration.class));
    }

    @Test
    void getFailCountReturnsZeroWhenKeyMissing() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:fail:user01")).thenReturn(null);

        assertThat(adapter.getFailCount("user01")).isZero();
        verify(valueOperations, never()).increment(anyString());
    }
}
