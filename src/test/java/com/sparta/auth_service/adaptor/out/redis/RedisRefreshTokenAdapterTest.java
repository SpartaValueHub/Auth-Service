package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.application.exception.SecurityStoreUnavailableException;
import com.sparta.auth_service.application.port.out.dto.RefreshTokenRotationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisRefreshTokenAdapter adapter;

    @Test
    void save_skipsRedisWriteWhenTtlIsZero() {
        adapter.save("uuid-001", "jti-001", 0L);

        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void save_skipsRedisWriteWhenAuthUuidOrTokenIdBlank() {
        adapter.save(null, "jti-001", 60L);
        adapter.save("", "jti-001", 60L);
        adapter.save("uuid-001", null, 60L);
        adapter.save("uuid-001", "", 60L);

        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void rotate_returnsJtiMismatchWhenTtlIsZero() {
        RefreshTokenRotationResult rotated = adapter.rotate("uuid-001", "old-jti", "new-jti", 0L);

        assertThat(rotated).isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);
        verify(stringRedisTemplate, never()).execute(any(), any(List.class), any());
    }

    @Test
    void rotate_returnsJtiMismatchWhenInputsBlank() {
        assertThat(adapter.rotate(null, "old-jti", "new-jti", 60L))
                .isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);
        assertThat(adapter.rotate("uuid-001", "", "new-jti", 60L))
                .isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);
        assertThat(adapter.rotate("uuid-001", "old-jti", null, 60L))
                .isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);

        verify(stringRedisTemplate, never()).execute(any(), any(List.class), any());
    }

    @Test
    void delete_skipsRedisWhenAuthUuidBlank() {
        adapter.delete(null);
        adapter.delete("");

        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    void save_writesWhenTtlIsPositive() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        adapter.save("uuid-001", "jti-001", 60L);

        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void save_throwsWhenRedisFails() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("redis down"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        assertThatThrownBy(() -> adapter.save("uuid-001", "jti-001", 60L))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }

    @Test
    void rotate_throwsWhenRedisExecuteFails() {
        when(stringRedisTemplate.execute(any(), anyList(), any(), any(), any()))
                .thenThrow(new RuntimeException("redis down"));

        assertThatThrownBy(() -> adapter.rotate("uuid-001", "old-jti", "new-jti", 60L))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }

    @Test
    void rotate_returnsJtiMismatchWhenJtiMismatch() {
        when(stringRedisTemplate.execute(any(), anyList(), any(), any(), any())).thenReturn(0L);

        assertThat(adapter.rotate("uuid-001", "old-jti", "new-jti", 60L))
                .isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);
    }

    @Test
    void rotate_returnsKeyNotFoundWhenRedisKeyMissing() {
        when(stringRedisTemplate.execute(any(), anyList(), any(), any(), any())).thenReturn(2L);

        assertThat(adapter.rotate("uuid-001", "old-jti", "new-jti", 60L))
                .isEqualTo(RefreshTokenRotationResult.KEY_NOT_FOUND);
    }

    @Test
    void rotate_returnsSuccessWhenRedisScriptReturnsOne() {
        when(stringRedisTemplate.execute(any(), anyList(), any(), any(), any())).thenReturn(1L);

        assertThat(adapter.rotate("uuid-001", "old-jti", "new-jti", 60L))
                .isEqualTo(RefreshTokenRotationResult.SUCCESS);
    }

    @Test
    void delete_throwsWhenRedisFails() {
        when(stringRedisTemplate.delete("auth:refresh:uuid-001")).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> adapter.delete("uuid-001"))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }

    @Test
    void deleteIfMatches_returnsFalseWhenInputsBlank() {
        assertThat(adapter.deleteIfMatches(null, "jti")).isFalse();
        assertThat(adapter.deleteIfMatches("uuid-001", null)).isFalse();

        verify(stringRedisTemplate, never()).execute(any(), anyList(), any());
    }

    @Test
    void deleteIfMatches_returnsTrueWhenRedisScriptReturnsOne() {
        when(stringRedisTemplate.execute(any(), anyList(), any())).thenReturn(1L);

        assertThat(adapter.deleteIfMatches("uuid-001", "jti-001")).isTrue();
    }

    @Test
    void deleteIfMatches_returnsFalseWhenRedisScriptReturnsZero() {
        when(stringRedisTemplate.execute(any(), anyList(), any())).thenReturn(0L);

        assertThat(adapter.deleteIfMatches("uuid-001", "jti-001")).isFalse();
    }

    @Test
    void deleteIfMatches_throwsWhenRedisExecuteFails() {
        when(stringRedisTemplate.execute(any(), anyList(), any()))
                .thenThrow(new RuntimeException("redis down"));

        assertThatThrownBy(() -> adapter.deleteIfMatches("uuid-001", "jti-001"))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }
}
