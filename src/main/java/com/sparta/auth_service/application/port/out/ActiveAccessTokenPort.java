package com.sparta.auth_service.application.port.out;

import java.util.Optional;

/** 계정당 1개 활성 Access Token jti (Redis) — 이중 로그인 시 이전 jti 블랙리스트용 */
public interface ActiveAccessTokenPort {

    Optional<String> find(String authUuid);

    void save(String authUuid, String tokenId, long ttlSeconds);

    void delete(String authUuid);
}
