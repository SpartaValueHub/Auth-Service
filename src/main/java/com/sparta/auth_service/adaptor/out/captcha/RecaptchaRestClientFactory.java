package com.sparta.auth_service.adaptor.out.captcha;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Google reCAPTCHA siteverify용 RestClient — connect/read 타임아웃 적용 */
final class RecaptchaRestClientFactory {

    private RecaptchaRestClientFactory() {
    }

    static RestClient create(CaptchaProperties captchaProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(captchaProperties.getConnectTimeoutMillis()));
        requestFactory.setReadTimeout(Duration.ofMillis(captchaProperties.getReadTimeoutMillis()));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
