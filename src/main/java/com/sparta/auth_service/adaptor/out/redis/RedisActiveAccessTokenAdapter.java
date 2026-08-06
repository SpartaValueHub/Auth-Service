package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.application.port.out.ActiveAccessTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 활성 Access Token jti — 1 authUuid : 1 tokenId (Redis).
 * 신규 로그인 시 이전 jti를 blacklist에 올려 Gateway에서 즉시 거부한다.
 * <p>
 * Redis 장애 정책: fail-closed. missing key → empty/false.
 */
@Component
@RequiredArgsConstructor
public class RedisActiveAccessTokenAdapter implements ActiveAccessTokenPort {

    private static final String KEY_PREFIX = "auth:access:";

    private static final DefaultRedisScript<Long> DELETE_IF_MATCHES_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('GET', KEYS[1])
            if not current then
              return 0
            end
            if current == ARGV[1] then
              redis.call('DEL', KEYS[1])
              return 1
            end
            return 0
            """,
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Optional<String> find(String authUuid) {
        return Optional.ofNullable(RedisSecurityStoreSupport.execute(() ->
                stringRedisTemplate.opsForValue().get(KEY_PREFIX + authUuid)
        ));
    }

    @Override
    public void save(String authUuid, String tokenId, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        RedisSecurityStoreSupport.run(() ->
                stringRedisTemplate.opsForValue().set(
                        KEY_PREFIX + authUuid,
                        tokenId,
                        Duration.ofSeconds(ttlSeconds)
                )
        );
    }

    @Override
    public void delete(String authUuid) {
        RedisSecurityStoreSupport.run(() -> stringRedisTemplate.delete(KEY_PREFIX + authUuid));
    }

    @Override
    public boolean deleteIfMatches(String authUuid, String expectedTokenId) {
        if (authUuid == null || authUuid.isBlank() || expectedTokenId == null || expectedTokenId.isBlank()) {
            return false;
        }
        Long result = RedisSecurityStoreSupport.execute(() ->
                stringRedisTemplate.execute(
                        DELETE_IF_MATCHES_SCRIPT,
                        List.of(KEY_PREFIX + authUuid),
                        expectedTokenId
                )
        );
        return result != null && result == 1L;
    }
}
