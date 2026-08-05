package com.sparta.auth_service.application.port.in;

import com.sparta.auth_service.application.port.in.dto.IdentityVerificationConfirmRequestDto;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationResultDto;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationStatusResultDto;

/** 본인인증 confirm·status Input Port */
public interface IdentityVerificationUseCase {

    IdentityVerificationResultDto confirm(IdentityVerificationConfirmRequestDto requestDto);

    IdentityVerificationStatusResultDto getStatus(String requestToken);
}
