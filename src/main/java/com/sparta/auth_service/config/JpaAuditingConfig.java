package com.sparta.auth_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Entity createdAt·updatedAt — Domain/Application에서 시간 생성하지 않음 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
