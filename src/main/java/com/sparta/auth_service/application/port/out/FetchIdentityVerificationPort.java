package com.sparta.auth_service.application.port.out;

import com.sparta.auth_service.application.port.out.dto.ExternalIdentityVerificationDto;

import java.util.Optional;

/** PortOne 본인인증 결과 조회 — CI·고객정보는 Application에서 sign-up·prefill에만 사용 */
public interface FetchIdentityVerificationPort {

    Optional<ExternalIdentityVerificationDto> fetchByRequestToken(String requestToken);
}
