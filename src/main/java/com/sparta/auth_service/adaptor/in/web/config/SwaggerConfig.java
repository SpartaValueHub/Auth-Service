package com.sparta.auth_service.adaptor.in.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 직접 접근 시 Swagger — 운영·FE 연동은 Gateway(/auth-service) 경유 권장 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .description("인증/회원가입 API 문서")
                        .version("v1.0.0"));
    }
}
