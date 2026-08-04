package com.sparta.auth_service.adaptor.out.portone;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** PortOne API 연동 설정 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "portone")
public class PortOneProperties {

    /** PortOne API Secret — Authorization 헤더 전용, 로그·응답에 포함 금지 */
    private String apiSecret;
    private String baseUrl = "https://api.portone.io";
}
