package com.sparta.auth_service.adaptor.out.redis;

import com.sparta.auth_service.adaptor.out.security.JwtProperties;
import com.sparta.auth_service.application.port.out.AccessTokenBlacklistPort;
import com.sparta.auth_service.application.port.out.ActiveAccessTokenPort;
import com.sparta.auth_service.application.port.out.RefreshTokenPort;
import com.sparta.auth_service.application.port.out.SessionInvalidationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Redis 세션 전체 revoke — inactive refresh·회원 탈퇴 등에서 공유 */
@Component
@RequiredArgsConstructor
public class RedisSessionInvalidationAdapter implements SessionInvalidationPort {

    private final ActiveAccessTokenPort activeAccessTokenPort;
    private final AccessTokenBlacklistPort accessTokenBlacklistPort;
    private final RefreshTokenPort refreshTokenPort;
    private final JwtProperties jwtProperties;

    @Override
    public void revokeAllSessions(String authUuid) {
        // Redis 세션 정리는 DB @Transactional과 원자적이지 않음 — 부분 실패 시 재시도·모니터링으로 보완.
        // 활성 access Redis 값에 expiresAt 없음 — parseable access token blacklist와 동일하게 설정 TTL 사용.
        activeAccessTokenPort.find(authUuid).ifPresent(jti ->
                accessTokenBlacklistPort.blacklist(jti, accessTokenTtlSeconds())
        );
        activeAccessTokenPort.delete(authUuid);
        refreshTokenPort.delete(authUuid);
    }

    private long accessTokenTtlSeconds() {
        return jwtProperties.getAccessTokenMinutes() * 60L;
    }
}
