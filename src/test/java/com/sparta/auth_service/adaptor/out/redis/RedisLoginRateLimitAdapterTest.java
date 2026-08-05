package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.adaptor.in.web.config.LoginRateLimitProperties;
import com.sparta.auth_service.application.port.out.dto.LoginRateLimitResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class RedisLoginRateLimitAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private LoginRateLimitProperties properties;
    private RedisLoginRateLimitAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new LoginRateLimitProperties();
        properties.setEnabled(true);
        properties.setMaxAttempts(20);
        properties.setWindowSeconds(60);
        properties.setBlockSeconds(60);
        adapter = new RedisLoginRateLimitAdapter(stringRedisTemplate, properties);
    }

    @Test
    void checkAndRecordReturnsAllowedWhenUnderLimit() {
        when(stringRedisTemplate.execute(any(), anyList(), any(), any(), any()))
                .thenReturn(List.of(1L, 0L));

        LoginRateLimitResultDto result = adapter.checkAndRecord("203.0.113.10");

        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    void checkAndRecordReturnsBlockedWithRetryAfter() {
        when(stringRedisTemplate.execute(any(), anyList(), any(), any(), any()))
                .thenReturn(List.of(0L, 45L));

        LoginRateLimitResultDto result = adapter.checkAndRecord("203.0.113.10");

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRetryAfterSeconds()).isEqualTo(45L);
    }

    @Test
    void checkAndRecordSkipsWhenDisabled() {
        properties.setEnabled(false);

        LoginRateLimitResultDto result = adapter.checkAndRecord("203.0.113.10");

        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    void checkAndRecordFailOpenOnUnexpectedLuaResult() {
        when(stringRedisTemplate.execute(any(), anyList(), any(), any(), any()))
                .thenReturn(List.of(1L));

        LoginRateLimitResultDto result = adapter.checkAndRecord("203.0.113.10");

        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    void checkAndRecordFailOpenOnNullLuaResult() {
        when(stringRedisTemplate.execute(any(), anyList(), any(), any(), any()))
                .thenReturn(null);

        LoginRateLimitResultDto result = adapter.checkAndRecord("203.0.113.10");

        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    void checkAndRecordFailOpenOnRedisErrorDoesNotLogPlainIp(CapturedOutput output) {
        when(stringRedisTemplate.execute(any(), anyList(), any(), any(), any()))
                .thenThrow(new RuntimeException("redis down"));

        LoginRateLimitResultDto result = adapter.checkAndRecord("203.0.113.10");

        assertThat(result.isAllowed()).isTrue();
        String logs = output.getOut() + output.getErr();
        assertThat(logs).contains("login_rate_limit_redis_failure");
        assertThat(logs).contains("fail_open");
        assertThat(logs).doesNotContain("203.0.113.10");
    }

    @Test
    void checkAndRecordFailOpenOnRedisError() {
        when(stringRedisTemplate.execute(any(), anyList(), any(), any(), any()))
                .thenThrow(new RuntimeException("redis down"));

        LoginRateLimitResultDto result = adapter.checkAndRecord("203.0.113.10");

        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    void hashIpIsDeterministicSha256Hex() {
        String first = RedisLoginRateLimitAdapter.hashIp("203.0.113.10");
        String second = RedisLoginRateLimitAdapter.hashIp("203.0.113.10");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }

    @Test
    void checkAndRecordUsesHashedIpInRedisKeys() {
        String ip = "203.0.113.10";
        String ipHash = RedisLoginRateLimitAdapter.hashIp(ip);
        when(stringRedisTemplate.execute(any(), anyList(), any(), any(), any()))
                .thenReturn(List.of(1L, 0L));

        adapter.checkAndRecord(ip);

        verify(stringRedisTemplate).execute(
                any(),
                eq(List.of("login:rate:count:" + ipHash, "login:rate:block:" + ipHash)),
                eq("20"),
                eq("60"),
                eq("60")
        );
    }
}
