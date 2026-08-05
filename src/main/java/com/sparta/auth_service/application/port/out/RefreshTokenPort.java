package com.sparta.auth_service.application.port.out;

/** Refresh Token jti 저장·atomic rotation (Redis) */
public interface RefreshTokenPort {

    void save(String authUuid, String tokenId, long ttlSeconds);

    /**
     * expectedTokenId와 일치할 때만 newTokenId로 교체. 동시 refresh 시 하나만 성공.
     *
     * @return rotation 성공 여부
     */
    boolean rotate(String authUuid, String expectedTokenId, String newTokenId, long ttlSeconds);

    void delete(String authUuid);
}
