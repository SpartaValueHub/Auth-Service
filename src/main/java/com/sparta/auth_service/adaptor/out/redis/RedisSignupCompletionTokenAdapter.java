package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.application.port.out.SignupCompletionTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisSignupCompletionTokenAdapter implements SignupCompletionTokenPort {
    public static final String KEY_PREFIX = "auth:signup-completion:";
    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String authUuid, String tokenId, long ttlSeconds) {
        if (authUuid == null || authUuid.isBlank() || tokenId == null || tokenId.isBlank() || ttlSeconds <= 0) {
            return;
        }
        RedisSecurityStoreSupport.run(() -> redisTemplate.opsForValue()
                .set(KEY_PREFIX + authUuid, tokenId, Duration.ofSeconds(ttlSeconds)));
    }
}
