package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.application.port.out.AccessTokenBlacklistPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 로그아웃된 Access Token jti 블랙리스트 — TTL은 토큰 잔여 만료와 동일.
 * Gateway blacklist 연동은 Edge Filter에서 isBlacklisted 호출.
 * <p>
 * Redis 장애 정책: fail-closed. missing key → false.
 */
@Component
@RequiredArgsConstructor
public class RedisAccessTokenBlacklistAdapter implements AccessTokenBlacklistPort {

    private static final String KEY_PREFIX = "auth:blacklist:access:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void blacklist(String tokenId, long ttlSeconds) {
        // 이미 만료된 토큰은 Redis에 적재하지 않음
        if (ttlSeconds <= 0) {
            return;
        }
        RedisSecurityStoreSupport.run(() ->
                stringRedisTemplate.opsForValue().set(
                        KEY_PREFIX + tokenId,
                        "1",
                        Duration.ofSeconds(ttlSeconds)
                )
        );
    }

    @Override
    public boolean isBlacklisted(String tokenId) {
        Boolean exists = RedisSecurityStoreSupport.execute(() ->
                stringRedisTemplate.hasKey(KEY_PREFIX + tokenId)
        );
        return Boolean.TRUE.equals(exists);
    }
}
