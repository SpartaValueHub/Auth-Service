package com.sparta.auth_service.application.exception;

/** member_status가 ACTIVE가 아님 — 403 AUTH_MEMBER_NOT_ACTIVE */
public class MemberNotActiveException extends RuntimeException {

    public MemberNotActiveException(String message) {
        super(message);
    }
}
