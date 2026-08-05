package com.sparta.auth_service.adaptor.in.web.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 로그인 실패·CAPTCHA·잠금 정책 — Redis TTL과 Application 임계값을 한곳에서 관리.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.login-attempt")
public class LoginAttemptProperties {

    /** fail count가 이 값에 도달하면 CAPTCHA_REQUIRED 응답 (기본 5 → 5번째 실패 직후) */
    private int captchaThreshold = 5;

    /** CAPTCHA 통과 후 이 횟수에 도달하면 잠금 (기본 6) */
    private int lockThreshold = 6;

    /** 잠금 지속 시간(분). TTL 만료 시 fail·lock 키 자동 삭제 */
    private int lockDurationMinutes = 1;

    /** 잠금 전 fail 카운트 유지 시간(분) */
    private int failCountWindowMinutes = 10;
}
