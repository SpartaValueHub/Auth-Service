package com.unionclass.auth_service.adaptor.in.web;

import com.unionclass.auth_service.adaptor.in.web.mapper.AuthWebMapper;
import com.unionclass.auth_service.adaptor.in.web.vo.AuthSignInRequestVo;
import com.unionclass.auth_service.adaptor.in.web.vo.AuthSignInResponseVo;
import com.unionclass.auth_service.adaptor.in.web.vo.AuthSignUpRequestVo;
import com.unionclass.auth_service.adaptor.in.web.vo.AuthSignUpResponseVo;
import com.unionclass.auth_service.application.port.in.AuthUseCase;
import com.unionclass.auth_service.application.port.in.dto.AuthSignInRequestDto;
import com.unionclass.auth_service.application.port.in.dto.AuthSignInResultDto;
import com.unionclass.auth_service.application.port.in.dto.AuthSignUpRequestDto;
import com.unionclass.auth_service.application.port.in.dto.AuthSignUpResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;
    private final AuthWebMapper authWebMapper;

    @Operation(summary = "회원가입", description = "신규 사용자를 등록합니다.")
    @PostMapping("/auth/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthSignUpResponseVo signUp(@RequestBody AuthSignUpRequestVo authSignUpRequestVo) {
        AuthSignUpRequestDto requestDto = authWebMapper.toDto(authSignUpRequestVo);
        AuthSignUpResultDto resultDto = authUseCase.signUp(requestDto);
        return authWebMapper.toVo(resultDto);
    }

    @Operation(summary = "로그인", description = "아이디/비밀번호로 로그인하고 JWT를 발급합니다.")
    @PostMapping("/auth/sign-in")
    public AuthSignInResponseVo signIn(@RequestBody AuthSignInRequestVo authSignInRequestVo) {
        AuthSignInRequestDto requestDto = authWebMapper.toDto(authSignInRequestVo);
        AuthSignInResultDto resultDto = authUseCase.signIn(requestDto);
        return authWebMapper.toVo(resultDto);
    }
}
