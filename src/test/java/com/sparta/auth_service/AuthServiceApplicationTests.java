package com.sparta.auth_service;

import com.sparta.auth_service.application.port.out.AccessTokenBlacklistPort;
import com.sparta.auth_service.application.port.out.RefreshTokenPort;
import com.sparta.auth_service.test.support.TestJwtKeyFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@ActiveProfiles("local")
class AuthServiceApplicationTests {

	@MockitoBean
	private RefreshTokenPort refreshTokenPort;

	@MockitoBean
	private AccessTokenBlacklistPort accessTokenBlacklistPort;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("portone.api-secret", () -> "test-portone-api-secret");
		registry.add("jwt.private-key", TestJwtKeyFixtures::privateKeyPem);
	}

	@Test
	void contextLoads() {
	}

}
