package com.sparta.auth_service.adaptor.in.web.mapper;

import com.sparta.auth_service.adaptor.in.web.vo.AuthSignInRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignInResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignUpRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.AuthSignUpResponseVo;
import com.sparta.auth_service.application.port.in.dto.AuthSignInRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpResultDto;
import org.springframework.stereotype.Component;

@Component
public class AuthWebMapper {

    public AuthSignUpRequestDto toDto(AuthSignUpRequestVo vo) {
        return AuthSignUpRequestDto.builder()
                .logInId(vo.getLogInId())
                .password(vo.getPassword())
                .email(vo.getEmail())
                .name(vo.getName())
                .phone(vo.getPhone())
                .build();
    }

    public AuthSignUpResponseVo toVo(AuthSignUpResultDto dto) {
        return AuthSignUpResponseVo.builder()
                .userId(dto.getUserId())
                .logInId(dto.getLogInId())
                .email(dto.getEmail())
                .name(dto.getName())
                .build();
    }

    public AuthSignInRequestDto toDto(AuthSignInRequestVo vo) {
        return AuthSignInRequestDto.builder()
                .logInId(vo.getLogInId())
                .password(vo.getPassword())
                .build();
    }

    public AuthSignInResponseVo toVo(AuthSignInResultDto dto) {
        return AuthSignInResponseVo.builder()
                .accessToken(dto.getAccessToken())
                .refreshToken(dto.getRefreshToken())
                .userId(dto.getUserId())
                .logInId(dto.getLogInId())
                .name(dto.getName())
                .email(dto.getEmail())
                .build();
    }
}
