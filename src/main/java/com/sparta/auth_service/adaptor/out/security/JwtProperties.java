package com.sparta.auth_service.adaptor.out.security;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** JWT RS256 키·만료 설정 — private-key는 Outbound Adapter만 사용 */
@Getter
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** PEM 본문 또는 jwt.private-key-location — Git·로그 노출 금지 */
    private final String privateKey;
    private final String privateKeyLocation;

    @Min(1)
    private final long accessTokenMinutes;

    @Min(1)
    private final long refreshTokenDays;

    public JwtProperties(
            String privateKey,
            String privateKeyLocation,
            @DefaultValue("15") @Min(1) long accessTokenMinutes,
            @DefaultValue("14") @Min(1) long refreshTokenDays
    ) {
        this.privateKey = privateKey;
        this.privateKeyLocation = privateKeyLocation;
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
    }
}
