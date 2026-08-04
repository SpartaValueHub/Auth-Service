package com.sparta.auth_service.application.port.out;

/** 로그아웃 Access Token jti 블랙리스트 (Redis TTL = 잔여 만료) */
public interface AccessTokenBlacklistPort {

    void blacklist(String tokenId, long ttlSeconds);

    boolean isBlacklisted(String tokenId);
}
