package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.adaptor.in.web.config.LoginAttemptProperties;
import com.sparta.auth_service.application.exception.SecurityStoreUnavailableException;
import com.sparta.auth_service.application.port.out.LoginAttemptPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 로그인 실패·잠금 — Redis TTL은 LoginAttemptProperties 기준.
 * login:fail:{loginId}, login:lock:{loginId}
 * <p>
 * Redis 장애 정책: fail-closed — {@link SecurityStoreUnavailableException} → HTTP 503.
 * 손상된 fail count는 0으로 취급하지 않음.
 */
@Component
@RequiredArgsConstructor
public class RedisLoginAttemptAdapter implements LoginAttemptPort {

    private static final String FAIL_KEY_PREFIX = "login:fail:";
    private static final String LOCK_KEY_PREFIX = "login:lock:";

    private final StringRedisTemplate stringRedisTemplate;
    private final LoginAttemptProperties loginAttemptProperties;

    @Override
    public int getFailCount(String loginId) {
        String value = RedisSecurityStoreSupport.execute(() ->
                stringRedisTemplate.opsForValue().get(failKey(loginId))
        );
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new SecurityStoreUnavailableException(ex);
        }
    }

    @Override
    public int incrementFailCount(String loginId) {
        Long count = RedisSecurityStoreSupport.execute(() ->
                stringRedisTemplate.opsForValue().increment(failKey(loginId))
        );
        if (count != null && count == 1L) {
            RedisSecurityStoreSupport.run(() ->
                    stringRedisTemplate.expire(failKey(loginId), failCountWindowTtl())
            );
        }
        if (count == null) {
            throw new SecurityStoreUnavailableException(
                    new IllegalStateException("Redis INCR returned null")
            );
        }
        return count.intValue();
    }

    @Override
    public boolean isLocked(String loginId) {
        return Boolean.TRUE.equals(RedisSecurityStoreSupport.execute(() ->
                stringRedisTemplate.hasKey(lockKey(loginId))
        ));
    }

    @Override
    public long getLockRemainingSeconds(String loginId) {
        Long ttl = RedisSecurityStoreSupport.execute(() ->
                stringRedisTemplate.getExpire(lockKey(loginId), TimeUnit.SECONDS)
        );
        if (ttl == null || ttl <= 0L) {
            return 0L;
        }
        return ttl;
    }

    @Override
    public void lock(String loginId) {
        Duration lockTtl = lockTtl();
        RedisSecurityStoreSupport.run(() -> {
            stringRedisTemplate.opsForValue().set(lockKey(loginId), "1", lockTtl);
            // 잠금 TTL 만료 시 fail 카운트도 함께 초기화
            stringRedisTemplate.expire(failKey(loginId), lockTtl);
        });
    }

    @Override
    public void reset(String loginId) {
        RedisSecurityStoreSupport.run(() -> {
            stringRedisTemplate.delete(failKey(loginId));
            stringRedisTemplate.delete(lockKey(loginId));
        });
    }

    private Duration failCountWindowTtl() {
        return Duration.ofMinutes(loginAttemptProperties.getFailCountWindowMinutes());
    }

    private Duration lockTtl() {
        return Duration.ofMinutes(loginAttemptProperties.getLockDurationMinutes());
    }

    private static String failKey(String loginId) {
        return FAIL_KEY_PREFIX + loginId;
    }

    private static String lockKey(String loginId) {
        return LOCK_KEY_PREFIX + loginId;
    }
}
