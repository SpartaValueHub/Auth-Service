package com.sparta.auth_service.application.port.out;

/** 가입에 사용된 CI hash를 DB unique 제약으로 원자적으로 선점한다. */
public interface SignupIdentityClaimPort {

    void claim(String ciHash, String authUuid);
}
