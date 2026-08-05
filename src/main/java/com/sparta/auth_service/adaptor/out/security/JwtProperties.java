package com.sparta.auth_service.adaptor.out.security;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** JWT RS256 키·만료 설정 — private-key는 Outbound Adapter만 사용 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** PEM 본문 또는 jwt.private-key-location — Git·로그 노출 금지 */
    private String privateKey;
    private String privateKeyLocation;

    @Min(1)
    private long accessTokenMinutes;

    @Min(1)
    private long refreshTokenDays;
}
