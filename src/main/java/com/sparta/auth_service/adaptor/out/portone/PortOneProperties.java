package com.sparta.auth_service.adaptor.out.portone;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** PortOne API 연동 설정 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "portone")
public class PortOneProperties {

    /** PortOne API Secret — Authorization 헤더 전용, 로그·응답에 포함 금지 */
    private String apiSecret;
    private String baseUrl = "https://api.portone.io";

    @Min(1)
    private int connectTimeoutMillis = 2000;

    @Min(1)
    private int readTimeoutMillis = 5000;
}
