package com.sparta.auth_service.adaptor.in.web.mapper;

import com.sparta.auth_service.adaptor.in.web.vo.AuthAvailabilityResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignInRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignInResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignUpRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignUpResponseVo;
import com.sparta.auth_service.application.port.in.dto.AuthAvailabilityResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpResultDto;
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
}
