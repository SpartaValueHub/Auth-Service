package com.sparta.auth_service.adaptor.in.web.support;

import com.sparta.auth_service.adaptor.in.web.config.AuthCookieProperties;
import com.sparta.auth_service.adaptor.out.security.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieWriterTest {

    private AuthCookieWriter authCookieWriter;

    @BeforeEach
    void setUp() {
        AuthCookieProperties cookieProperties = new AuthCookieProperties(
                "vh_access_token",
                "vh_refresh_token",
                "/",
                "/",
                "",
                false,
                "Lax"
        );

        JwtProperties jwtProperties = new JwtProperties(null, null, 15L, 14L, 120L);

        authCookieWriter = new AuthCookieWriter(cookieProperties, jwtProperties);
    }

    @Test
    void accessAndRefreshCookiesUseRootPath() {
        ResponseCookie access = authCookieWriter.accessTokenCookie("access-value");
        ResponseCookie refresh = authCookieWriter.refreshTokenCookie("refresh-value");

        assertThat(access.getPath()).isEqualTo("/");
        assertThat(refresh.getPath()).isEqualTo("/");
    }

    @Test
    void clearCookiesUseSamePathsAsSetCookies() {
        ResponseCookie clearAccess = authCookieWriter.clearAccessTokenCookie();
        ResponseCookie clearRefresh = authCookieWriter.clearRefreshTokenCookie();

        assertThat(clearAccess.getPath()).isEqualTo("/");
        assertThat(clearRefresh.getPath()).isEqualTo("/");
        assertThat(clearAccess.getMaxAge().getSeconds()).isZero();
        assertThat(clearRefresh.getMaxAge().getSeconds()).isZero();
    }
}
