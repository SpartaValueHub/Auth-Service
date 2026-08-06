package com.sparta.auth_service.adaptor.out.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * docker-compose Redis(localhost:6379) 실 Lua deleteIfMatches 검증.
 */
class RedisActiveAccessTokenAdapterIntegrationTest {

    private static final String KEY_PREFIX = "auth:access:";

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate stringRedisTemplate;

    private RedisActiveAccessTokenAdapter adapter;
    private String authUuid;

    @BeforeAll
    static void requireRedis() {
        try {
            connectionFactory = new LettuceConnectionFactory("localhost", 6379);
            connectionFactory.afterPropertiesSet();
            stringRedisTemplate = new StringRedisTemplate(connectionFactory);
            stringRedisTemplate.afterPropertiesSet();
            String pong = connectionFactory.getConnection().ping();
            Assumptions.assumeTrue("PONG".equalsIgnoreCase(pong),
                    "Redis not available at localhost:6379 — run: docker compose up -d");
        } catch (Exception ex) {
            Assumptions.assumeTrue(false,
                    "Redis not available at localhost:6379 — run: docker compose up -d");
        }
    }

    @BeforeEach
    void setUp() {
        authUuid = "test-" + UUID.randomUUID();
        adapter = new RedisActiveAccessTokenAdapter(stringRedisTemplate);
    }

    @AfterEach
    void tearDown() {
        stringRedisTemplate.delete(KEY_PREFIX + authUuid);
    }

    @Test
    void deleteIfMatches_deletesWhenExpectedTokenMatches() {
        adapter.save(authUuid, "stored-jti", 60L);

        assertThat(adapter.deleteIfMatches(authUuid, "stored-jti")).isTrue();
        assertThat(stringRedisTemplate.hasKey(KEY_PREFIX + authUuid)).isFalse();
    }

    @Test
    void deleteIfMatches_failsOnMismatchWithoutDeletingKey() {
        adapter.save(authUuid, "stored-jti", 60L);

        assertThat(adapter.deleteIfMatches(authUuid, "wrong-jti")).isFalse();
        assertThat(stringRedisTemplate.opsForValue().get(KEY_PREFIX + authUuid)).isEqualTo("stored-jti");
    }

    @Test
    void deleteIfMatches_returnsFalseWhenKeyMissing() {
        assertThat(adapter.deleteIfMatches(authUuid, "missing-jti")).isFalse();
    }
}
