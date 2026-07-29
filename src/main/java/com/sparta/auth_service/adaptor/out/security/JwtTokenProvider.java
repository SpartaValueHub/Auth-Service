package com.unionclass.auth_service.adaptor.out.security;

import com.unionclass.auth_service.application.port.out.TokenProviderPort;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import javax.crypto.SecretKey;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider implements TokenProviderPort {

    private static final String FALLBACK_SECRET =
            "auth-service-default-jwt-secret-key-change-me-please";

    private final JwtProperties properties;

    @Override
    public String createAccessToken(String userId, String logInId, String name) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + properties.getAccessTokenMinutes() * 60_000L);
        return Jwts.builder()
                .setSubject(userId)
                .claim("logInId", logInId)
                .claim("name", name)
                .claim("tokenType", "access")
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String createRefreshToken(String userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + properties.getRefreshTokenDays() * 24 * 60 * 60_000L);
        return Jwts.builder()
                .setSubject(userId)
                .claim("tokenType", "refresh")
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    private SecretKey key() {
        String raw = properties.getSecret();
        if (raw == null || raw.isBlank()) {
            raw = FALLBACK_SECRET;
        }
        byte[] bytes = raw.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            bytes = sha256(raw);
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
