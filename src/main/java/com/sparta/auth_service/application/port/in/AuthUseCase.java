package com.sparta.auth_service.application.port.in;

import com.sparta.auth_service.application.port.in.dto.AuthAvailabilityResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthLogoutRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthRefreshRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpResultDto;

/**
 * 인증 Input Port — HTTP·상태코드 없음.
 * JWT 발급·검증 분리: 발급은 auth-service, Edge 검증은 Gateway.
 */
public interface AuthUseCase {

    AuthSignUpResultDto signUp(AuthSignUpRequestDto authSignUpRequestDto);

    AuthSignInResultDto signIn(AuthSignInRequestDto authSignInRequestDto);

    AuthSignInResultDto refresh(AuthRefreshRequestDto requestDto);

    void logout(AuthLogoutRequestDto requestDto);

    AuthAvailabilityResultDto checkLoginIdAvailability(String loginId);

    AuthAvailabilityResultDto checkEmailAvailability(String email);
}
