package com.sparta.auth_service.adaptor.in.web.support;

import com.sparta.auth_service.adaptor.in.web.config.AuthCookieProperties;
import com.sparta.auth_service.adaptor.out.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/** sign-in·refresh Set-Cookie / logout 쿠키 삭제 */
@Component
@RequiredArgsConstructor
public class AuthCookieWriter {

    private final AuthCookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    public ResponseCookie accessTokenCookie(String accessToken) {
        return buildCookie(
                cookieProperties.getAccessName(),
                accessToken,
                cookieProperties.getPath(),
                Duration.ofMinutes(jwtProperties.getAccessTokenMinutes())
        );
    }

    public ResponseCookie refreshTokenCookie(String refreshToken) {
        return buildCookie(
                cookieProperties.getRefreshName(),
                refreshToken,
                cookieProperties.getRefreshPath(),
                Duration.ofDays(jwtProperties.getRefreshTokenDays())
        );
    }

    public ResponseCookie clearAccessTokenCookie() {
        return buildCookie(cookieProperties.getAccessName(), "", cookieProperties.getPath(), Duration.ZERO);
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return buildCookie(
                cookieProperties.getRefreshName(),
                "",
                cookieProperties.getRefreshPath(),
                Duration.ZERO
        );
    }

    private ResponseCookie buildCookie(String name, String value, String path, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path(path)
                .maxAge(maxAge);

        if (StringUtils.hasText(cookieProperties.getDomain())) {
            builder.domain(cookieProperties.getDomain());
        }

        String sameSite = cookieProperties.getSameSite();
        if (StringUtils.hasText(sameSite)) {
            builder.sameSite(sameSite);
        }

        return builder.build();
    }
}
