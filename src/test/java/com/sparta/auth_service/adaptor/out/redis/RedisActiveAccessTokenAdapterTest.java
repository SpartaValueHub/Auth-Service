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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisActiveAccessTokenAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisActiveAccessTokenAdapter adapter;

    @Test
    void save_skipsRedisWriteWhenTtlIsZero() {
        adapter.save("uuid-001", "jti-001", 0L);

        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void save_skipsRedisWriteWhenTtlIsNegative() {
        adapter.save("uuid-001", "jti-001", -1L);

        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void save_writesWhenTtlIsPositive() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        adapter.save("uuid-001", "jti-001", 60L);

        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void find_throwsWhenRedisFails() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:access:uuid-001")).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> adapter.find("uuid-001"))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }

    @Test
    void save_throwsWhenRedisFails() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("down"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        assertThatThrownBy(() -> adapter.save("uuid-001", "jti-001", 60L))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }

    @Test
    void delete_throwsWhenRedisFails() {
        when(stringRedisTemplate.delete("auth:access:uuid-001")).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> adapter.delete("uuid-001"))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }
}
