package com.sparta.auth_service.application.port.out;

public interface SignupCompletionTokenPort {
    void save(String authUuid, String tokenId, long ttlSeconds);
}
