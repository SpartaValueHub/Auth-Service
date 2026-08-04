package com.sparta.auth_service.application.port.out;

/** Refresh Token jti 저장·rotation 검증 (Redis) */
public interface RefreshTokenPort {

    void save(String authUuid, String tokenId, long ttlSeconds);

    boolean matches(String authUuid, String tokenId);

    void delete(String authUuid);
}
