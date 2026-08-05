package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.application.port.out.ActiveAccessTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
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
}
