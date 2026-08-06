package com.sparta.auth_service.adaptor.in.web;

import com.sparta.auth_service.adaptor.in.web.mapper.AuthWebMapper;
import com.sparta.auth_service.adaptor.in.web.support.AuthCookieWriter;
import com.sparta.auth_service.adaptor.in.web.support.ClientIpResolver;
import com.sparta.auth_service.adaptor.in.web.vo.AuthAvailabilityResponseVo;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 인증 Inbound Controller — VO↔UseCase 위임만, 비즈니스·트랜잭션은 Application.
 * JWT는 HttpOnly Cookie로 발급·갱신·삭제. Gateway Edge에서 Cookie JWT 검증.
 */
@Tag(name = "Auth", description = "인증 API — JWT 발급·계정. 닉네임/프로필은 member-service")
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;
    private final AuthWebMapper authWebMapper;
    private final AuthCookieWriter authCookieWriter;
    private final ClientIpResolver clientIpResolver;

    @Operation(summary = "회원가입", description = "본인인증 SUCCESS 후 회원을 등록합니다.")
    @PostMapping("/auth/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthSignUpResponseVo signUp(@RequestBody AuthSignUpRequestVo authSignUpRequestVo) {
        AuthSignUpRequestDto requestDto = authWebMapper.toDto(authSignUpRequestVo);
        AuthSignUpResultDto resultDto = authUseCase.signUp(requestDto);
        return authWebMapper.toVo(resultDto);
    }

    @Operation(summary = "로그인", description = "아이디/비밀번호로 로그인하고 JWT를 HttpOnly Cookie로 발급합니다.")
    @PostMapping("/auth/sign-in")
    public ResponseEntity<AuthSignInResponseVo> signIn(
            @RequestBody AuthSignInRequestVo authSignInRequestVo,
            HttpServletRequest httpServletRequest
    ) {
        String clientIp = clientIpResolver.resolve(httpServletRequest);
        AuthSignInRequestDto requestDto = authWebMapper.toDto(authSignInRequestVo, clientIp);
        AuthSignInResultDto resultDto = authUseCase.signIn(requestDto);
        return tokenResponse(resultDto);
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token Cookie로 Access/Refresh Token을 재발급합니다.")
    @PostMapping("/auth/refresh")
    public ResponseEntity<AuthSignInResponseVo> refresh(
            @CookieValue(name = "${auth.cookie.refresh-name:vh_refresh_token}", required = false) String refreshCookie,
            @CookieValue(name = "${auth.cookie.access-name:vh_access_token}", required = false) String accessCookie
    ) {
        AuthRefreshRequestDto requestDto = new AuthRefreshRequestDto(refreshCookie, accessCookie);
        AuthSignInResultDto resultDto = authUseCase.refresh(requestDto);
        return tokenResponse(resultDto);
    }

    @Operation(summary = "세션 유효성 확인", description = "Gateway JWT·blacklist 통과 시 204. 다른 기기 로그인 시 Gateway 401 AUTH_SESSION_TERMINATED.")
    @GetMapping("/auth/session")
    public ResponseEntity<Void> sessionStatus() {
        return ResponseEntity.noContent()
                .header("Cache-Control", "no-store")
                .build();
    }

    @Operation(summary = "로그아웃", description = "Refresh Token 무효화·Access Token 블랙리스트·Cookie 삭제")
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${auth.cookie.access-name:vh_access_token}", required = false) String accessCookie,
            @CookieValue(name = "${auth.cookie.refresh-name:vh_refresh_token}", required = false) String refreshCookie
    ) {
        AuthLogoutRequestDto requestDto = AuthLogoutRequestDto.builder()
                .accessToken(accessCookie)
                .refreshToken(refreshCookie)
                .build();
        authUseCase.logout(requestDto);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieWriter.clearAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authCookieWriter.clearRefreshTokenCookie().toString())
                .build();
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

    private ResponseEntity<AuthSignInResponseVo> tokenResponse(AuthSignInResultDto resultDto) {
        AuthSignInResponseVo body = authWebMapper.toVo(resultDto);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieWriter.accessTokenCookie(resultDto.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, authCookieWriter.refreshTokenCookie(resultDto.getRefreshToken()).toString())
                .body(body);
    }
}
