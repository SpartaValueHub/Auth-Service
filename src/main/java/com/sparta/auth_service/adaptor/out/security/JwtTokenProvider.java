package com.sparta.auth_service.adaptor.out.security;

import com.sparta.auth_service.application.exception.InvalidTokenException;
import com.sparta.auth_service.application.port.out.TokenProviderPort;
import com.sparta.auth_service.application.port.out.dto.ParsedTokenDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT RS256 발급. Gateway는 jwt-public.pem 으로 검증.
 * access claim: sub(authUuid), tokenType — PII·nickname 미포함(member-service 경계).
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider implements TokenProviderPort {

    private final JwtProperties properties;
    private final JwtRsaKeyLoader jwtRsaKeyLoader;
    private PrivateKey privateKey;

    @PostConstruct
    void init() {
        if (properties.getAccessTokenMinutes() <= 0 || properties.getRefreshTokenDays() <= 0) {
            throw new IllegalStateException("jwt access/refresh duration must be positive");
        }
        privateKey = jwtRsaKeyLoader.loadPrivateKey(properties);
    }

    @Override
    public String createAccessToken(String authUuid) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + properties.getAccessTokenMinutes() * 60_000L);
        return Jwts.builder()
                .setId(UUID.randomUUID().toString()) // jti — logout blacklist 키
                .setSubject(authUuid)
                .claim("tokenType", "access")
                .claim("role", "USER")
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    @Override
    public String createRefreshToken(String authUuid) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + properties.getRefreshTokenDays() * 24 * 60 * 60_000L);
        return Jwts.builder()
                .setId(UUID.randomUUID().toString()) // jti — Redis refresh matches 키
                .setSubject(authUuid)
                .claim("tokenType", "refresh")
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    @Override
    public String createSignupCompletionToken(String authUuid) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + properties.getSignupCompletionTokenSeconds() * 1_000L);
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(authUuid)
                .claim("tokenType", "SIGNUP_COMPLETION")
                .claim("purpose", "MEMBER_PROFILE_CREATE")
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    @Override
    public ParsedTokenDto parseRefreshToken(String refreshToken) {
        try {
            Jws<Claims> parsed = Jwts.parserBuilder()
                    .setSigningKey(privateKey)
                    .build()
                    .parseClaimsJws(refreshToken);
            Claims claims = parsed.getBody();
            if (!"refresh".equals(claims.get("tokenType", String.class))) {
                // access token을 refresh 엔드포인트에 쓰는 경우 차단
                throw new InvalidTokenException("유효하지 않은 refresh token입니다.");
            }
            return ParsedTokenDto.builder()
                    .tokenId(claims.getId())
                    .authUuid(claims.getSubject())
                    .tokenType("refresh")
                    .expiresAt(claims.getExpiration().toInstant())
                    .build();
        } catch (InvalidTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidTokenException("유효하지 않은 refresh token입니다.");
        }
    }

    @Override
    public ParsedTokenDto parseAccessToken(String accessToken) {
        try {
            Jws<Claims> parsed = Jwts.parserBuilder()
                    .setSigningKey(privateKey)
                    .build()
                    .parseClaimsJws(accessToken);
            Claims claims = parsed.getBody();
            if (!"access".equals(claims.get("tokenType", String.class))) {
                throw new InvalidTokenException("유효하지 않은 access token입니다.");
            }
            return ParsedTokenDto.builder()
                    .tokenId(claims.getId())
                    .authUuid(claims.getSubject())
                    .tokenType("access")
                    .expiresAt(claims.getExpiration().toInstant())
                    .build();
        } catch (InvalidTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidTokenException("유효하지 않은 access token입니다.");
        }
    }


    @Override
    public ParsedTokenDto parseSignupCompletionToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(privateKey).build()
                    .parseClaimsJws(token).getBody();
            if (!"SIGNUP_COMPLETION".equals(claims.get("tokenType", String.class))
                    || !"MEMBER_PROFILE_CREATE".equals(claims.get("purpose", String.class))) {
                throw new InvalidTokenException("유효하지 않은 가입 완료 토큰입니다.");
            }
            return ParsedTokenDto.builder()
                    .tokenId(claims.getId())
                    .authUuid(claims.getSubject())
                    .tokenType("SIGNUP_COMPLETION")
                    .expiresAt(claims.getExpiration().toInstant())
                    .build();
        } catch (InvalidTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidTokenException("유효하지 않은 가입 완료 토큰입니다.");
        }
    }
}
