package com.sparta.auth_service.adaptor.in.web.config;

import com.sparta.auth_service.adaptor.in.web.support.AuthOriginVerificationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** SecurityFilterChain 단일 등록 — servlet filter 중복 실행 방지 */
@Configuration
class AuthOriginFilterConfig {

    @Bean
    FilterRegistrationBean<AuthOriginVerificationFilter> disableAuthOriginServletFilterRegistration(
            AuthOriginVerificationFilter filter
    ) {
        FilterRegistrationBean<AuthOriginVerificationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
