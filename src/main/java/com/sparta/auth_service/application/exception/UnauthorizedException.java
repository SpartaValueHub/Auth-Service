package com.sparta.auth_service.application.exception;

/** 로그인 실패 — 존재 여부·비밀번호 오류 구분 없이 동일 외부 메시지(계정 열거 방지) */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
