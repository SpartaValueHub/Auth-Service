package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.adaptor.in.web.config.LoginRateLimitProperties;
import com.sparta.auth_service.application.port.out.dto.LoginRateLimitResultDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * docker-compose Redis(localhost:6379) 실 Lua·TTL·동시성 검증.
 * Mockito mock execute로는 원자성·TTL을 증명할 수 없음.
 */
class RedisLoginRateLimitAdapterIntegrationTest {

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate stringRedisTemplate;

    private LoginRateLimitProperties properties;
    private RedisLoginRateLimitAdapter adapter;
    private String testIp;

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
        testIp = "203.0.113." + (100 + Math.abs(UUID.randomUUID().hashCode() % 100));
        properties = new LoginRateLimitProperties(true, 20, 60, 60);
        adapter = new RedisLoginRateLimitAdapter(stringRedisTemplate, properties);
    }

    @AfterEach
    void tearDown() {
        String ipHash = RedisLoginRateLimitAdapter.hashIp(testIp);
        stringRedisTemplate.delete("login:rate:count:" + ipHash);
        stringRedisTemplate.delete("login:rate:block:" + ipHash);
    }

    @Test
    void allowsFirstTwentyBlocksTwentyFirst() {
        for (int i = 1; i <= 20; i++) {
            LoginRateLimitResultDto result = adapter.checkAndRecord(testIp);
            assertThat(result.isAllowed())
                    .as("request %d should be allowed", i)
                    .isTrue();
        }

        LoginRateLimitResultDto blocked = adapter.checkAndRecord(testIp);
        assertThat(blocked.isAllowed()).isFalse();
        assertThat(blocked.getRetryAfterSeconds()).isBetween(1L, 60L);
    }

    @Test
    void retryAfterWithinBlockSecondsRange() {
        for (int i = 0; i < 21; i++) {
            adapter.checkAndRecord(testIp);
        }

        LoginRateLimitResultDto blocked = adapter.checkAndRecord(testIp);
        assertThat(blocked.isAllowed()).isFalse();
        assertThat(blocked.getRetryAfterSeconds()).isBetween(1L, (long) properties.getBlockSeconds());
    }

    @Test
    void countKeyHasWindowTtl() {
        adapter.checkAndRecord(testIp);

        String ipHash = RedisLoginRateLimitAdapter.hashIp(testIp);
        Long ttl = stringRedisTemplate.getExpire("login:rate:count:" + ipHash, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(1L, (long) properties.getWindowSeconds());
    }

    @Test
    void blockKeyHasBlockTtlAfterLimitExceeded() {
        for (int i = 0; i < 21; i++) {
            adapter.checkAndRecord(testIp);
        }

        String ipHash = RedisLoginRateLimitAdapter.hashIp(testIp);
        Long ttl = stringRedisTemplate.getExpire("login:rate:block:" + ipHash, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(1L, (long) properties.getBlockSeconds());
    }

    @Test
    void allowsAgainAfterBlockAndWindowExpire() throws InterruptedException {
        properties = new LoginRateLimitProperties(true, 20, 2, 2);
        adapter = new RedisLoginRateLimitAdapter(stringRedisTemplate, properties);

        for (int i = 0; i < 21; i++) {
            adapter.checkAndRecord(testIp);
        }
        assertThat(adapter.checkAndRecord(testIp).isAllowed()).isFalse();

        Thread.sleep(2_500L);

        assertThat(adapter.checkAndRecord(testIp).isAllowed()).isTrue();
    }

    @Test
    void concurrentRequestsAllowAtMostMaxAttempts() throws InterruptedException {
        int threadCount = 30;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger blocked = new AtomicInteger();
        List<LoginRateLimitResultDto> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    LoginRateLimitResultDto result = adapter.checkAndRecord(testIp);
                    synchronized (results) {
                        results.add(result);
                    }
                    if (result.isAllowed()) {
                        allowed.incrementAndGet();
                    } else {
                        blocked.incrementAndGet();
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

        assertThat(allowed.get()).isEqualTo(20);
        assertThat(blocked.get()).isEqualTo(10);
    }

    @Test
    void differentIpsAreIndependent() {
        String otherIp = "203.0.113." + (50 + Math.abs(UUID.randomUUID().hashCode() % 50));
        String otherHash = RedisLoginRateLimitAdapter.hashIp(otherIp);

        try {
            for (int i = 0; i < 21; i++) {
                adapter.checkAndRecord(testIp);
            }
            assertThat(adapter.checkAndRecord(testIp).isAllowed()).isFalse();
            assertThat(adapter.checkAndRecord(otherIp).isAllowed()).isTrue();
        } finally {
            stringRedisTemplate.delete("login:rate:count:" + otherHash);
            stringRedisTemplate.delete("login:rate:block:" + otherHash);
        }
    }
}
