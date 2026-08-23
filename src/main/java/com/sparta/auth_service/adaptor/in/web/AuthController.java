package com.sparta.auth_service.adaptor.in.web;

import com.sparta.auth_service.adaptor.in.web.mapper.AuthWebMapper;
import com.sparta.auth_service.adaptor.in.web.support.AuthCookieWriter;
import com.sparta.auth_service.adaptor.in.web.support.ClientIpResolver;
import com.sparta.auth_service.adaptor.in.web.vo.AuthAccountResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.MemberJoinedAtResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthAvailabilityResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignInRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignInResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignUpRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignUpResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignUpResumeRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignUpResumeResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.WithdrawMemberRequestVo;
import com.sparta.auth_service.application.exception.UnauthorizedException;
import com.sparta.auth_service.application.port.in.AuthUseCase;
import com.sparta.auth_service.application.port.in.GetMemberJoinedAtUseCase;
import com.sparta.auth_service.application.port.in.GetMyAuthAccountUseCase;
import com.sparta.auth_service.application.port.in.WithdrawMemberUseCase;
import com.sparta.auth_service.application.port.in.dto.AuthAvailabilityResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthLogoutRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthRefreshRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpResumeRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpResumeResultDto;
import com.sparta.auth_service.application.port.in.dto.GetMemberJoinedAtResultDto;
import com.sparta.auth_service.application.port.in.dto.GetMyAuthAccountResultDto;
import com.sparta.auth_service.application.port.in.dto.WithdrawMemberRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    // Gateway가 JWT sub를 주입하는 회원(인증) UUID 헤더
    private static final String MEMBER_UUID_HEADER = "X-Member-Uuid";

    private final AuthUseCase authUseCase;
    private final GetMyAuthAccountUseCase getMyAuthAccountUseCase;
    private final GetMemberJoinedAtUseCase getMemberJoinedAtUseCase;
    private final WithdrawMemberUseCase withdrawMemberUseCase;
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

    @PostMapping("/auth/sign-up/resume")
    public AuthSignUpResumeResponseVo resumeSignUp(
            @RequestBody AuthSignUpResumeRequestVo requestVo,
            HttpServletRequest httpServletRequest
    ) {
        String clientIp = clientIpResolver.resolve(httpServletRequest);
        AuthSignUpResumeRequestDto requestDto = authWebMapper.toDto(requestVo, clientIp);
        AuthSignUpResumeResultDto resultDto = authUseCase.resumeSignUp(requestDto);
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

    @Operation(summary = "내 계정 정보 조회", description = "Gateway JWT 검증 후 X-Member-Uuid로 아이디·이메일·전화·가입일을 조회합니다.")
    @GetMapping("/auth/me")
    public AuthAccountResponseVo getMyAuthAccount(
            @RequestHeader(value = MEMBER_UUID_HEADER, required = false) String headerMemberUuid
    ) {
        String authUuid = requireMemberUuid(headerMemberUuid);
        GetMyAuthAccountResultDto resultDto = getMyAuthAccountUseCase.getMyAuthAccount(authUuid);
        return authWebMapper.toVo(resultDto);
    }


    @Operation(summary = "회원 가입일 조회", description = "ACTIVE 계정의 가입일(auth.created_at)을 조회합니다. Gateway public. WITHDRAWN/SUSPENDED/없음은 404.")
    @GetMapping("/auth/members/{memberUuid}/joined-at")
    public MemberJoinedAtResponseVo getMemberJoinedAt(@PathVariable String memberUuid) {
        GetMemberJoinedAtResultDto resultDto = getMemberJoinedAtUseCase.getMemberJoinedAt(memberUuid);
        return authWebMapper.toVo(resultDto);
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "purpose=WITHDRAWAL 본인인증 confirm SUCCESS 후 requestToken으로 탈퇴합니다. 가입 CI와 일치해야 합니다."
    )
    @PostMapping("/auth/withdraw")
    public ResponseEntity<Void> withdraw(
            @RequestHeader(value = MEMBER_UUID_HEADER, required = false) String headerMemberUuid,
            @RequestBody WithdrawMemberRequestVo requestVo
    ) {
        String authUuid = requireMemberUuid(headerMemberUuid);
        WithdrawMemberRequestDto requestDto = authWebMapper.toDto(requestVo, authUuid);
        withdrawMemberUseCase.withdraw(requestDto);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieWriter.clearAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authCookieWriter.clearRefreshTokenCookie().toString())
                .build();
    }

    private String requireMemberUuid(String headerMemberUuid) {
        if (headerMemberUuid == null || headerMemberUuid.isBlank()) {
            throw new UnauthorizedException("인증 정보가 없습니다.");
        }
        return headerMemberUuid.trim();
    }

    private ResponseEntity<AuthSignInResponseVo> tokenResponse(AuthSignInResultDto resultDto) {
        AuthSignInResponseVo body = authWebMapper.toVo(resultDto);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieWriter.accessTokenCookie(resultDto.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, authCookieWriter.refreshTokenCookie(resultDto.getRefreshToken()).toString())
                .body(body);
    }
}
