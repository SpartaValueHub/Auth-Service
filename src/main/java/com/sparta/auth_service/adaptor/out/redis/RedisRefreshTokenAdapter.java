package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.application.port.out.RefreshTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Refresh Token 1 authUuid : 1 tokenId (Redis).
 * rotation 시 delete 후 save — matches 로 Redis·JWT jti 일치 검증.
 */
@Component
@RequiredArgsConstructor
public class RedisRefreshTokenAdapter implements RefreshTokenPort {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(String authUuid, String tokenId, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(
                KEY_PREFIX + authUuid,
                tokenId,
                Duration.ofSeconds(ttlSeconds)
        );
    }

    @Override
    public boolean matches(String authUuid, String tokenId) {
        String stored = stringRedisTemplate.opsForValue().get(KEY_PREFIX + authUuid);
        return tokenId != null && tokenId.equals(stored);
    }

    @Override
    public void delete(String authUuid) {
        stringRedisTemplate.delete(KEY_PREFIX + authUuid);
    }
}
