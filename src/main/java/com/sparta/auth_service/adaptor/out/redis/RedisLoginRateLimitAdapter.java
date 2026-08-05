package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.adaptor.in.web.config.LoginRateLimitProperties;
import com.sparta.auth_service.application.port.out.LoginRateLimitPort;
import com.sparta.auth_service.application.port.out.dto.LoginRateLimitResultDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * IP sign-in rate limit — login:rate:count:{ipHash}, login:rate:block:{ipHash}
 * Lua로 block 확인·increment·TTL을 원자 처리.
 * <p>
 * Redis 장애 정책: fail-open — {@link LoginRateLimitResultDto#allowed()}.
 */
@Component
@RequiredArgsConstructor
public class RedisLoginRateLimitAdapter implements LoginRateLimitPort {

    private static final Logger log = LoggerFactory.getLogger(RedisLoginRateLimitAdapter.class);

    private static final String COUNT_KEY_PREFIX = "login:rate:count:";
    private static final String BLOCK_KEY_PREFIX = "login:rate:block:";

    private static final DefaultRedisScript<List> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            """
                    local blockTtl = redis.call('TTL', KEYS[2])
                    if blockTtl > 0 then
                      return {0, blockTtl}
                    end
                    local count = redis.call('INCR', KEYS[1])
                    if count == 1 then
                      redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
                    end
                    if count > tonumber(ARGV[1]) then
                      redis.call('SET', KEYS[2], '1', 'EX', tonumber(ARGV[3]))
                      return {0, tonumber(ARGV[3])}
                    end
                    return {1, 0}
                    """,
            List.class
    );

    private final StringRedisTemplate stringRedisTemplate;
    private final LoginRateLimitProperties loginRateLimitProperties;

    @Override
    public LoginRateLimitResultDto checkAndRecord(String clientIp) {
        if (!loginRateLimitProperties.isEnabled()) {
            return LoginRateLimitResultDto.allowed();
        }
        if (clientIp == null || clientIp.isBlank()) {
            return LoginRateLimitResultDto.allowed();
        }

        try {
            String ipHash = hashIp(clientIp);
            List<Long> result = stringRedisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    List.of(countKey(ipHash), blockKey(ipHash)),
                    String.valueOf(loginRateLimitProperties.getMaxAttempts()),
                    String.valueOf(loginRateLimitProperties.getWindowSeconds()),
                    String.valueOf(loginRateLimitProperties.getBlockSeconds())
            );

            if (result == null || result.size() < 2) {
                log.warn("login_rate_limit_unexpected_lua_result event=fail_open");
                return LoginRateLimitResultDto.allowed();
            }

            long allowedFlag = result.get(0);
            long retryAfterSeconds = result.get(1);
            if (allowedFlag == 1L) {
                return LoginRateLimitResultDto.allowed();
            }
            return LoginRateLimitResultDto.blocked(Math.max(1L, retryAfterSeconds));
        } catch (Exception ex) {
            log.warn("login_rate_limit_redis_failure event=fail_open exception={}", ex.getClass().getSimpleName());
            log.debug("login_rate_limit_redis_failure detail", ex);
            return LoginRateLimitResultDto.allowed();
        }
    }

    static String hashIp(String clientIp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(clientIp.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private static String countKey(String ipHash) {
        return COUNT_KEY_PREFIX + ipHash;
    }

    private static String blockKey(String ipHash) {
        return BLOCK_KEY_PREFIX + ipHash;
    }
}
