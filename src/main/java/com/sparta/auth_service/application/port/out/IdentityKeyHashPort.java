package com.sparta.auth_service.application.port.out;

/** CI(identity key) 검색·중복 검사용 결정적 HMAC 해시 */
public interface IdentityKeyHashPort {

    String hashForLookup(String identityKey);
}
