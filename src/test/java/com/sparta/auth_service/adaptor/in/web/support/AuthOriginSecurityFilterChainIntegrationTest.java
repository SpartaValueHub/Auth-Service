package com.sparta.auth_service.adaptor.in.web.support;

import com.sparta.auth_service.application.port.out.AccessTokenBlacklistPort;
import com.sparta.auth_service.application.port.out.ActiveAccessTokenPort;
import com.sparta.auth_service.application.port.out.CaptchaVerificationPort;
import com.sparta.auth_service.application.port.out.IdentityKeyHashPort;
import com.sparta.auth_service.application.port.out.LoginAttemptPort;
import com.sparta.auth_service.application.port.out.LoginRateLimitPort;
import com.sparta.auth_service.application.port.out.RefreshTokenPort;
import com.sparta.auth_service.test.support.TestJwtKeyFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "spring.datasource.hikari.maximum-pool-size=2",
        "captcha.enabled=false"
})
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "auth.origin.require-origin=true",
        "auth.origin.allowed-origins=http://localhost:3000"
})
class AuthOriginSecurityFilterChainIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private RefreshTokenPort refreshTokenPort;

    @MockitoBean
    private ActiveAccessTokenPort activeAccessTokenPort;

    @MockitoBean
    private AccessTokenBlacklistPort accessTokenBlacklistPort;

    @MockitoBean
    private LoginAttemptPort loginAttemptPort;

    @MockitoBean
    private LoginRateLimitPort loginRateLimitPort;

    @MockitoBean
    private CaptchaVerificationPort captchaVerificationPort;

    @MockitoBean
    private IdentityKeyHashPort identityKeyHashPort;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("portone.api-secret", () -> "test-portone-api-secret");
        registry.add("jwt.private-key", TestJwtKeyFixtures::privateKeyPem);
        registry.add("security.ci.hash-key", () -> "dGVzdC1oYXNoLWtleS0zMi1ieXRlcy0xMjM0");
        registry.add("auth.origin.require-origin", () -> "true");
        registry.add("auth.origin.allowed-origins", () -> "http://localhost:3000");
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void refreshWithoutOrigin_isBlockedBySecurityFilterChain() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ORIGIN"));
    }

    @Test
    void logoutWithoutOrigin_isBlockedBySecurityFilterChain() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ORIGIN"));
    }

    @Test
    void signInWithoutOrigin_isNotBlockedByOriginFilter() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-in"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }
}
