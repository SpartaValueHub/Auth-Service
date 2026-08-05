package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.application.exception.SecurityStoreUnavailableException;

import java.util.function.Supplier;

/**
 * Redis security store 공통 fail-closed 래퍼.
 * IP rate limit은 사용하지 않음(fail-open 정책 별도).
 */
final class RedisSecurityStoreSupport {

    private RedisSecurityStoreSupport() {
    }

    static <T> T execute(Supplier<T> action) {
        try {
            return action.get();
        } catch (SecurityStoreUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SecurityStoreUnavailableException(ex);
        }
    }

    static void run(Runnable action) {
        execute(() -> {
            action.run();
            return null;
        });
    }
}
