package com.sparta.auth_service.application.port.in;

import com.sparta.auth_service.application.port.in.dto.AuthSignInRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpResultDto;

public interface AuthUseCase {

    AuthSignUpResultDto signUp(AuthSignUpRequestDto authSignUpRequestDto);

    AuthSignInResultDto signIn(AuthSignInRequestDto authSignInRequestDto);
}
