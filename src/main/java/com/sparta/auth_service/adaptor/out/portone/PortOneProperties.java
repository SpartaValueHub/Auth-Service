package com.sparta.auth_service.adaptor.out.portone;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** PortOne API 연동 설정 */
@Getter
@Validated
@ConfigurationProperties(prefix = "portone")
public class PortOneProperties {

    /** PortOne API Secret — Authorization 헤더 전용, 로그·응답에 포함 금지 */
    private final String apiSecret;
    private final String baseUrl;

    @Min(1)
    private final int connectTimeoutMillis;

    @Min(1)
    private final int readTimeoutMillis;

    public PortOneProperties(
            String apiSecret,
            @DefaultValue("https://api.portone.io") String baseUrl,
            @DefaultValue("2000") @Min(1) int connectTimeoutMillis,
            @DefaultValue("5000") @Min(1) int readTimeoutMillis
    ) {
        this.apiSecret = apiSecret;
        this.baseUrl = baseUrl;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }
}
