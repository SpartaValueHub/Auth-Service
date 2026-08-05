package com.sparta.auth_service.adaptor.out.captcha;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class RecaptchaClientConfig {

    @Bean
    RestClient recaptchaRestClient(CaptchaProperties captchaProperties) {
        return RecaptchaRestClientFactory.create(captchaProperties);
    }
}
