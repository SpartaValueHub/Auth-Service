package com.sparta.auth_service.application.port.out;

import com.sparta.auth_service.application.exception.CaptchaProviderUnavailableException;

/**
 * 로그인 captcha 토큰 검증.
 * <p>
 * 사용자 검증 실패(success=false, hostname 등) → {@code false}.
 * 제공자 장애(타임아웃·네트워크·5xx·파싱 실패) → {@link CaptchaProviderUnavailableException}.
 */
public interface CaptchaVerificationPort {

    /**
     * @return true if captcha valid; false if user validation failed
     * @throws CaptchaProviderUnavailableException if Google siteverify is unavailable
     */
    boolean verify(String captchaToken);
}
