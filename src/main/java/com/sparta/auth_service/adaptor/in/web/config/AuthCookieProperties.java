package com.sparta.auth_service.adaptor.in.web.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** HttpOnly JWT 쿠키 설정 — Gateway·FE cross-origin 시 domain·sameSite 조정 */
@Getter
@ConfigurationProperties(prefix = "auth.cookie")
public class AuthCookieProperties {

    private final String accessName;
    private final String refreshName;
    private final String path;
    /** refresh 쿠키 path — access와 동일하게 / 로 두어 Next BFF(/api/auth/*)에서 읽을 수 있게 함 */
    private final String refreshPath;
    /** 비우면 host-only (localhost 등). prod에서는 .example.com 등 설정 */
    private final String domain;
    private final boolean secure;
    /** Lax | Strict | None — cross-origin fetch 시 None+Secure 필요 */
    private final String sameSite;

    public AuthCookieProperties(
            @DefaultValue("vh_access_token") String accessName,
            @DefaultValue("vh_refresh_token") String refreshName,
            @DefaultValue("/") String path,
            @DefaultValue("/") String refreshPath,
            @DefaultValue("") String domain,
            @DefaultValue("false") boolean secure,
            @DefaultValue("Lax") String sameSite
    ) {
        this.accessName = accessName;
        this.refreshName = refreshName;
        this.path = path;
        this.refreshPath = refreshPath;
        this.domain = domain;
        this.secure = secure;
        this.sameSite = sameSite;
    }
}
