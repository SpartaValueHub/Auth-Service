package com.sparta.auth_service.application.port.out;

import com.sparta.auth_service.application.port.out.dto.RefreshTokenRotationResult;

/** Refresh Token jti 저장·atomic rotation (Redis) */
public interface RefreshTokenPort {

    void save(String authUuid, String tokenId, long ttlSeconds);

    /**
     * expectedTokenId와 일치할 때만 newTokenId로 교체. 동시 refresh 시 하나만 성공.
     *
     * @return SUCCESS / KEY_NOT_FOUND(Redis 키 없음) / JTI_MISMATCH(다른 jti·동시 refresh 패배)
     */
    RefreshTokenRotationResult rotate(String authUuid, String expectedTokenId, String newTokenId, long ttlSeconds);

    void delete(String authUuid);

    /**
     * Redis에 저장된 tokenId가 expectedTokenId와 일치할 때만 키 삭제.
     * stale logout이 최신 세션 키를 지우지 않도록 compare-and-delete.
     *
     * @return 일치하여 삭제했으면 true, 키 없음·불일치·blank 입력이면 false
     */
    boolean deleteIfMatches(String authUuid, String expectedTokenId);
}
