package com.sparta.auth_service.adaptor.out.captcha;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** captcha.enabled=true 일 때 secret-key 필수 */
@Component
@RequiredArgsConstructor
class CaptchaEnabledSecretValidator {

    private final CaptchaProperties captchaProperties;

    @PostConstruct
    void validate() {
        if (captchaProperties.isEnabled()
                && (captchaProperties.getRecaptcha().getSecretKey() == null
                || captchaProperties.getRecaptcha().getSecretKey().isBlank())) {
            throw new IllegalStateException("captcha.recaptcha.secret-key must be configured when captcha.enabled=true");
        }
    }
}
