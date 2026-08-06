package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.adaptor.in.web.config.LoginAttemptProperties;
import com.sparta.auth_service.adaptor.in.web.config.LoginRateLimitProperties;
import com.sparta.auth_service.application.exception.SecurityStoreUnavailableException;
import com.sparta.auth_service.application.port.out.dto.LoginRateLimitResultDto;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * stopped/wrong-port Redis — security store 503, IP rate limit fail-open.
 */
class RedisSecurityStoreFailureIntegrationTest {

    private static LettuceConnectionFactory goodConnectionFactory;
    private static StringRedisTemplate goodRedisTemplate;

    private LoginRateLimitProperties rateLimitProperties;
    private LoginAttemptProperties loginAttemptProperties;
    private RedisLoginRateLimitAdapter rateLimitAdapter;
    private RedisLoginAttemptAdapter loginAttemptAdapter;

    @BeforeAll
    static void requireGoodRedis() {
        try {
            goodConnectionFactory = new LettuceConnectionFactory("localhost", 6379);
            goodConnectionFactory.afterPropertiesSet();
            goodRedisTemplate = new StringRedisTemplate(goodConnectionFactory);
            goodRedisTemplate.afterPropertiesSet();
            String pong = goodConnectionFactory.getConnection().ping();
            Assumptions.assumeTrue("PONG".equalsIgnoreCase(pong),
                    "Redis not available at localhost:6379 — skipped");
        } catch (Exception ex) {
            Assumptions.assumeTrue(false, "Redis not available at localhost:6379 — skipped");
        }
    }

    @BeforeEach
    void setUp() {
        rateLimitProperties = new LoginRateLimitProperties(true, 20, 60, 60);
        loginAttemptProperties = new LoginAttemptProperties(5, 6, 1, 10);
    }

    @Test
    void loginRateLimitFailOpenWhenRedisUnavailable() {
        StringRedisTemplate deadRedis = deadRedisTemplate();
        rateLimitAdapter = new RedisLoginRateLimitAdapter(deadRedis, rateLimitProperties);

        LoginRateLimitResultDto result = rateLimitAdapter.checkAndRecord("203.0.113.50");

        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    void loginAttemptFailClosedWhenRedisUnavailable() {
        StringRedisTemplate deadRedis = deadRedisTemplate();
        loginAttemptAdapter = new RedisLoginAttemptAdapter(deadRedis, loginAttemptProperties);

        assertThatThrownBy(() -> loginAttemptAdapter.getFailCount("user01"))
                .isInstanceOf(SecurityStoreUnavailableException.class);
    }

    @Test
    void loginAttemptWorksWhenRedisAvailable() {
        rateLimitAdapter = new RedisLoginRateLimitAdapter(goodRedisTemplate, rateLimitProperties);
        loginAttemptAdapter = new RedisLoginAttemptAdapter(goodRedisTemplate, loginAttemptProperties);

        assertThat(loginAttemptAdapter.getFailCount("integration-user-" + System.nanoTime())).isZero();
        assertThat(rateLimitAdapter.checkAndRecord("203.0.113." + (System.nanoTime() % 100)).isAllowed()).isTrue();
    }

    private static StringRedisTemplate deadRedisTemplate() {
        LettuceConnectionFactory deadFactory = new LettuceConnectionFactory("localhost", 6390);
        deadFactory.setValidateConnection(false);
        deadFactory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(deadFactory);
        template.afterPropertiesSet();
        return template;
    }
}
