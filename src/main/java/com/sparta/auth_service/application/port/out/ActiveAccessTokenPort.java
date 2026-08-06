package com.sparta.auth_service.application.port.out;

import java.util.Optional;

/** 계정당 1개 활성 Access Token jti (Redis) — 이중 로그인 시 이전 jti 블랙리스트용 */
public interface ActiveAccessTokenPort {

    Optional<String> find(String authUuid);

    void save(String authUuid, String tokenId, long ttlSeconds);

    void delete(String authUuid);

    /**
     * Redis에 저장된 tokenId가 expectedTokenId와 일치할 때만 키 삭제.
     * stale logout이 최신 활성 access jti를 지우지 않도록 compare-and-delete.
     *
     * @return 일치하여 삭제했으면 true, 키 없음·불일치·blank 입력이면 false
     */
    boolean deleteIfMatches(String authUuid, String expectedTokenId);
}
