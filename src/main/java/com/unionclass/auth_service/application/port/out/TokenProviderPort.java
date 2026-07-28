package com.unionclass.auth_service.application.port.out;

public interface TokenProviderPort {

    String createAccessToken(String userId, String logInId, String name);

    String createRefreshToken(String userId);
}
