package com.sparta.auth_service.adaptor.in.web;

import com.sparta.auth_service.adaptor.in.web.config.DependencyFailureProperties;

/** GlobalExceptionHandler 단위 테스트용 기본 설정 */
final class GlobalExceptionHandlerTestSupport {

    private GlobalExceptionHandlerTestSupport() {
    }

    static GlobalExceptionHandler handler() {
        DependencyFailureProperties properties = new DependencyFailureProperties(5);
        return new GlobalExceptionHandler(properties);
    }
}
