package com.sparta.auth_service.adaptor.in.web.mapper;

import com.sparta.auth_service.adaptor.in.web.vo.AuthAccountResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthAvailabilityResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignInRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignInResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignUpRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignUpResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignUpResumeRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignUpResumeResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.WithdrawMemberRequestVo;
import com.sparta.auth_service.application.port.in.dto.AuthAvailabilityResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpResumeRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpResumeResultDto;
import com.sparta.auth_service.application.port.in.dto.GetMyAuthAccountResultDto;
import com.sparta.auth_service.application.port.in.dto.WithdrawMemberRequestDto;
import org.springframework.stereotype.Component;

/** VO ↔ Input DTO 변환 — sign-up은 requestToken·loginId·password·email만 전달 */
@Component
public class AuthWebMapper {

    public AuthSignUpRequestDto toDto(AuthSignUpRequestVo vo) {
        return AuthSignUpRequestDto.builder()
                .requestToken(vo.getRequestToken())
                .loginId(vo.getLogInId())
                .password(vo.getPassword())
                .email(vo.getEmail())
                .build();
    }

    public AuthSignUpResponseVo toVo(AuthSignUpResultDto dto) {
        return AuthSignUpResponseVo.builder()
                .signupCompletionToken(dto.getSignupCompletionToken())
                .authUuid(dto.getAuthUuid())
                .logInId(dto.getLoginId())
                .email(dto.getEmail())
                .memberName(dto.getMemberName())
                .birthdayDate(dto.getBirthdayDate())
                .build();
    }

    public AuthSignInRequestDto toDto(AuthSignInRequestVo vo, String clientIp) {
        return AuthSignInRequestDto.builder()
                .loginId(vo.getLogInId())
                .password(vo.getPassword())
                .captchaToken(vo.getCaptchaToken())
                .clientIp(clientIp)
                .build();
    }

    public AuthSignUpResumeRequestDto toDto(AuthSignUpResumeRequestVo vo, String clientIp) {
        return AuthSignUpResumeRequestDto.builder()
                .loginId(vo.getLogInId())
                .password(vo.getPassword())
                .captchaToken(vo.getCaptchaToken())
                .clientIp(clientIp)
                .build();
    }

    public AuthSignUpResumeResponseVo toVo(AuthSignUpResumeResultDto dto) {
        return AuthSignUpResumeResponseVo.builder()
                .authUuid(dto.getAuthUuid())
                .signupCompletionToken(dto.getSignupCompletionToken())
                .build();
    }

    public AuthSignInResponseVo toVo(AuthSignInResultDto dto) {
        return AuthSignInResponseVo.builder()
                .memberUuid(dto.getAuthUuid())
                .nickname(dto.getMemberName())
                .role(dto.getRole())
                .build();
    }

    public AuthAvailabilityResponseVo toVo(AuthAvailabilityResultDto dto) {
        return AuthAvailabilityResponseVo.builder()
                .available(dto.isAvailable())
                .build();
    }

    public AuthAccountResponseVo toVo(GetMyAuthAccountResultDto dto) {
        return AuthAccountResponseVo.builder()
                .authUuid(dto.getAuthUuid())
                .logInId(dto.getLoginId())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .joinedAt(dto.getJoinedAt())
                .build();
    }

    public WithdrawMemberRequestDto toDto(WithdrawMemberRequestVo vo, String authUuid) {
        return WithdrawMemberRequestDto.builder()
                .authUuid(authUuid)
                .requestToken(vo.getRequestToken())
                .build();
    }
}
