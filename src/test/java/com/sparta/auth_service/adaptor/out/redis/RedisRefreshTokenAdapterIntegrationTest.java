package com.sparta.auth_service.adaptor.out.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sparta.auth_service.application.port.out.dto.RefreshTokenRotationResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * docker-compose Redis(localhost:6379) 실 Lua rotate·TTL·동시성 검증.
 */
class RedisRefreshTokenAdapterIntegrationTest {

    private static final String KEY_PREFIX = "auth:refresh:";

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate stringRedisTemplate;

    private RedisRefreshTokenAdapter adapter;
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
        adapter = new RedisRefreshTokenAdapter(stringRedisTemplate);
    }

    @AfterEach
    void tearDown() {
        stringRedisTemplate.delete(KEY_PREFIX + authUuid);
    }

    @Test
    void rotate_succeedsWhenExpectedTokenMatches() {
        adapter.save(authUuid, "old-jti", 60L);

        RefreshTokenRotationResult rotated = adapter.rotate(authUuid, "old-jti", "new-jti", 60L);

        assertThat(rotated).isEqualTo(RefreshTokenRotationResult.SUCCESS);
        assertThat(stringRedisTemplate.opsForValue().get(KEY_PREFIX + authUuid)).isEqualTo("new-jti");
    }

    @Test
    void rotate_setsTtlWithinRequestedRange() {
        adapter.save(authUuid, "old-jti", 60L);

        adapter.rotate(authUuid, "old-jti", "new-jti", 120L);

        Long ttl = stringRedisTemplate.getExpire(KEY_PREFIX + authUuid, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(1L, 120L);
    }

    @Test
    void rotate_failsOnMismatchWithoutChangingValue() {
        adapter.save(authUuid, "stored-jti", 60L);

        RefreshTokenRotationResult rotated = adapter.rotate(authUuid, "wrong-jti", "new-jti", 60L);

        assertThat(rotated).isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);
        assertThat(stringRedisTemplate.opsForValue().get(KEY_PREFIX + authUuid)).isEqualTo("stored-jti");
    }

    @Test
    void rotate_failsWhenKeyMissing() {
        RefreshTokenRotationResult rotated = adapter.rotate(authUuid, "old-jti", "new-jti", 60L);

        assertThat(rotated).isEqualTo(RefreshTokenRotationResult.KEY_NOT_FOUND);
    }

    @Test
    void rotate_returnsFalseWhenTtlIsZeroOrNegative() {
        adapter.save(authUuid, "old-jti", 60L);

        assertThat(adapter.rotate(authUuid, "old-jti", "new-jti", 0L))
                .isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);
        assertThat(adapter.rotate(authUuid, "old-jti", "new-jti", -1L))
                .isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);
        assertThat(stringRedisTemplate.opsForValue().get(KEY_PREFIX + authUuid)).isEqualTo("old-jti");
    }

    @Test
    void rotate_returnsFalseForNullOrBlankInputs() {
        adapter.save(authUuid, "old-jti", 60L);

        assertThat(adapter.rotate(null, "old-jti", "new-jti", 60L))
                .isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);
        assertThat(adapter.rotate("", "old-jti", "new-jti", 60L))
                .isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);
        assertThat(adapter.rotate("  ", "old-jti", "new-jti", 60L))
                .isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);
        assertThat(adapter.rotate(authUuid, null, "new-jti", 60L))
                .isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);
        assertThat(adapter.rotate(authUuid, "", "new-jti", 60L))
                .isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);
        assertThat(adapter.rotate(authUuid, "old-jti", null, 60L))
                .isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);
        assertThat(adapter.rotate(authUuid, "old-jti", "", 60L))
                .isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);

        assertThat(stringRedisTemplate.opsForValue().get(KEY_PREFIX + authUuid)).isEqualTo("old-jti");
    }

    @Test
    void save_skipsRedisForNullOrBlankInputs() {
        adapter.save(null, "jti", 60L);
        adapter.save("", "jti", 60L);
        adapter.save(authUuid, null, 60L);
        adapter.save(authUuid, "", 60L);
        adapter.save(authUuid, "jti", 0L);

        assertThat(stringRedisTemplate.hasKey(KEY_PREFIX + authUuid)).isFalse();
    }

    @Test
    void delete_skipsRedisForNullOrBlankAuthUuid() {
        adapter.save(authUuid, "jti", 60L);

        adapter.delete(null);
        adapter.delete("");
        adapter.delete("  ");

        assertThat(stringRedisTemplate.opsForValue().get(KEY_PREFIX + authUuid)).isEqualTo("jti");
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

    @Test
    void sequentialTwoRotations_onlyFirstSucceeds() {
        adapter.save(authUuid, "old-jti", 60L);

        assertThat(adapter.rotate(authUuid, "old-jti", "new-jti-1", 60L))
                .isEqualTo(RefreshTokenRotationResult.SUCCESS);
        assertThat(adapter.rotate(authUuid, "old-jti", "new-jti-2", 60L))
                .isEqualTo(RefreshTokenRotationResult.JTI_MISMATCH);

        assertThat(stringRedisTemplate.opsForValue().get(KEY_PREFIX + authUuid)).isEqualTo("new-jti-1");
    }

    @Test
    void concurrentRotations_allowExactlyOneSuccess() throws InterruptedException {
        adapter.save(authUuid, "old-jti", 60L);

        int threadCount = 25;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        List<String> newJtis = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            String newJti = "new-jti-" + i;
            executor.submit(() -> {
                try {
                    start.await();
                    if (adapter.rotate(authUuid, "old-jti", newJti, 60L) == RefreshTokenRotationResult.SUCCESS) {
                        successCount.incrementAndGet();
                        synchronized (newJtis) {
                            newJtis.add(newJti);
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(newJtis).hasSize(1);
        assertThat(stringRedisTemplate.opsForValue().get(KEY_PREFIX + authUuid)).isEqualTo(newJtis.get(0));
    }

    @Test
    void differentAuthUuids_rotateIndependently() {
        String otherAuthUuid = "test-" + UUID.randomUUID();
        try {
            adapter.save(authUuid, "jti-a", 60L);
            adapter.save(otherAuthUuid, "jti-b", 60L);

            assertThat(adapter.rotate(authUuid, "jti-a", "new-a", 60L))
                    .isEqualTo(RefreshTokenRotationResult.SUCCESS);
            assertThat(adapter.rotate(otherAuthUuid, "jti-b", "new-b", 60L))
                    .isEqualTo(RefreshTokenRotationResult.SUCCESS);

            assertThat(stringRedisTemplate.opsForValue().get(KEY_PREFIX + authUuid)).isEqualTo("new-a");
            assertThat(stringRedisTemplate.opsForValue().get(KEY_PREFIX + otherAuthUuid)).isEqualTo("new-b");
        } finally {
            stringRedisTemplate.delete(KEY_PREFIX + otherAuthUuid);
        }
    }

    @Test
    void save_keyExpiresAfterTtl() throws InterruptedException {
        adapter.save(authUuid, "jti", 2L);

        assertThat(stringRedisTemplate.opsForValue().get(KEY_PREFIX + authUuid)).isEqualTo("jti");

        Thread.sleep(2_500L);

        assertThat(stringRedisTemplate.hasKey(KEY_PREFIX + authUuid)).isFalse();
        assertThat(adapter.rotate(authUuid, "jti", "new-jti", 60L))
                .isEqualTo(RefreshTokenRotationResult.KEY_NOT_FOUND);
    }
}
