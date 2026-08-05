package com.sparta.auth_service;

import com.sparta.auth_service.application.port.out.AccessTokenBlacklistPort;
import com.sparta.auth_service.application.port.out.ActiveAccessTokenPort;
import com.sparta.auth_service.application.port.out.CaptchaVerificationPort;
import com.sparta.auth_service.application.port.out.LoginAttemptPort;
import com.sparta.auth_service.application.port.out.LoginRateLimitPort;
import com.sparta.auth_service.application.port.out.RefreshTokenPort;
import com.sparta.auth_service.application.port.out.IdentityKeyHashPort;
import com.sparta.auth_service.test.support.TestJwtKeyFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
		"spring.datasource.hikari.maximum-pool-size=2",
		"captcha.enabled=false"
})
@ActiveProfiles("local")
class AuthServiceApplicationTests {

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

	@Test
	void contextLoads() {
	}

}
