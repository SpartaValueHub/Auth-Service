package com.sparta.auth_service.adaptor.in.web.config;

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
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "spring.datasource.hikari.maximum-pool-size=2",
        "captcha.enabled=false"
})
@ActiveProfiles("local")
class LocalProfileConfigurationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private Environment environment;

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
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void jpaSettingsMatchLocalDevelopmentExpectations() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(environment.getProperty("spring.jpa.show-sql", Boolean.class)).isTrue();
        assertThat(environment.getProperty("spring.jpa.properties.hibernate.format_sql", Boolean.class)).isTrue();
    }

    @Test
    void apiDocsEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
