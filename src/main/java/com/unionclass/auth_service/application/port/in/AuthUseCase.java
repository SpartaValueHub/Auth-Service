package com.unionclass.auth_service.application.port.in;

import com.unionclass.auth_service.application.port.in.dto.AuthSignInRequestDto;
import com.unionclass.auth_service.application.port.in.dto.AuthSignInResultDto;
import com.unionclass.auth_service.application.port.in.dto.AuthSignUpRequestDto;
import com.unionclass.auth_service.application.port.in.dto.AuthSignUpResultDto;

public interface AuthUseCase {

    AuthSignUpResultDto signUp(AuthSignUpRequestDto authSignUpRequestDto);

    AuthSignInResultDto signIn(AuthSignInRequestDto authSignInRequestDto);
}
