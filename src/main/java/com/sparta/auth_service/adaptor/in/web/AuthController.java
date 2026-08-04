package com.sparta.auth_service.adaptor.in.web;

import com.sparta.auth_service.adaptor.in.web.mapper.AuthWebMapper;
import com.sparta.auth_service.adaptor.in.web.vo.AuthAvailabilityResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthLogoutRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthRefreshRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignInRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignInResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignUpRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignUpResponseVo;
import com.sparta.auth_service.application.port.in.AuthUseCase;
import com.sparta.auth_service.application.port.in.dto.AuthAvailabilityResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthLogoutRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthRefreshRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 Inbound Controller — VO↔UseCase 위임만, 비즈니스·트랜잭션은 Application.
 * JWT 검증·CORS는 Gateway Edge; auth-service API는 Gateway 경유 public.
 */
@Tag(name = "Auth", description = "인증 API — JWT 발급·계정. 닉네임/프로필은 member-service")
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;
    private final AuthWebMapper authWebMapper;

    @Operation(summary = "회원가입", description = "본인인증 SUCCESS 후 회원을 등록합니다.")
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

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 Access/Refresh Token을 재발급합니다.")
    @PostMapping("/auth/refresh")
    public AuthSignInResponseVo refresh(@RequestBody AuthRefreshRequestVo requestVo) {
        AuthRefreshRequestDto requestDto = authWebMapper.toDto(requestVo);
        AuthSignInResultDto resultDto = authUseCase.refresh(requestDto);
        return authWebMapper.toVo(resultDto);
    }

    @Operation(summary = "로그아웃", description = "Refresh Token을 무효화하고 Access Token을 블랙리스트에 등록합니다.")
    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody AuthLogoutRequestVo requestVo) {
        AuthLogoutRequestDto requestDto = authWebMapper.toDto(requestVo);
        authUseCase.logout(requestDto);
    }

    @Operation(summary = "아이디 중복 확인")
    @GetMapping("/auth/check/login-id")
    public AuthAvailabilityResponseVo checkLoginId(@RequestParam String loginId) {
        AuthAvailabilityResultDto resultDto = authUseCase.checkLoginIdAvailability(loginId);
        return authWebMapper.toVo(resultDto);
    }

    @Operation(summary = "이메일 중복 확인")
    @GetMapping("/auth/check/email")
    public AuthAvailabilityResponseVo checkEmail(@RequestParam String email) {
        AuthAvailabilityResultDto resultDto = authUseCase.checkEmailAvailability(email);
        return authWebMapper.toVo(resultDto);
   }
}
