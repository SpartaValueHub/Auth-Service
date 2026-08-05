package com.sparta.auth_service.adaptor.in.web.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** HttpOnly JWT 쿠키 설정 — Gateway·FE cross-origin 시 domain·sameSite 조정 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.cookie")
public class AuthCookieProperties {

    private String accessName = "vh_access_token";
    private String refreshName = "vh_refresh_token";
    private String path = "/";
    /** refresh 쿠키 path — access와 동일하게 / 로 두어 Next BFF(/api/auth/*)에서 읽을 수 있게 함 */
    private String refreshPath = "/";
    /** 비우면 host-only (localhost 등). prod에서는 .example.com 등 설정 */
    private String domain = "";
    private boolean secure = false;
    /** Lax | Strict | None — cross-origin fetch 시 None+Secure 필요 */
    private String sameSite = "Lax";
}
