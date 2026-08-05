package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.application.port.out.RefreshTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Refresh Token 1 authUuid : 1 tokenId (Redis).
 * rotate는 Lua로 GET-compare-SET을 atomic 수행한다.
 * <p>
 * Redis 장애 정책: fail-closed. logical mismatch(JTI 불일치) → false(InvalidTokenException).
 * missing key → rotate false.
 */
@Component
@RequiredArgsConstructor
public class RedisRefreshTokenAdapter implements RefreshTokenPort {

    private static final String KEY_PREFIX = "auth:refresh:";

    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('GET', KEYS[1])
            if current == ARGV[1] then
              redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
              return 1
            end
            return 0
            """,
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(String authUuid, String tokenId, long ttlSeconds) {
        if (!hasText(authUuid) || !hasText(tokenId) || ttlSeconds <= 0) {
            return;
        }
        RedisSecurityStoreSupport.run(() ->
                stringRedisTemplate.opsForValue().set(
                        key(authUuid),
                        tokenId,
                        Duration.ofSeconds(ttlSeconds)
                )
        );
    }

    @Override
    public boolean rotate(String authUuid, String expectedTokenId, String newTokenId, long ttlSeconds) {
        if (!hasText(authUuid) || !hasText(expectedTokenId) || !hasText(newTokenId) || ttlSeconds <= 0) {
            return false;
        }
        Long result = RedisSecurityStoreSupport.execute(() ->
                stringRedisTemplate.execute(
                        ROTATE_SCRIPT,
                        List.of(key(authUuid)),
                        expectedTokenId,
                        newTokenId,
                        String.valueOf(ttlSeconds)
                )
        );
        return result != null && result == 1L;
    }

    @Override
    public void delete(String authUuid) {
        if (!hasText(authUuid)) {
            return;
        }
        RedisSecurityStoreSupport.run(() -> stringRedisTemplate.delete(key(authUuid)));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String key(String authUuid) {
        return KEY_PREFIX + authUuid;
    }
}
