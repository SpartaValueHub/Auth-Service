package com.unionclass.auth_service.adaptor.in.web.mapper;

import com.unionclass.auth_service.adaptor.in.web.vo.AuthSignUpRequestVo;
import com.unionclass.auth_service.adaptor.in.web.vo.AuthSignUpResponseVo;
import com.unionclass.auth_service.application.port.in.dto.AuthSignUpRequestDto;
import com.unionclass.auth_service.application.port.in.dto.AuthSignUpResultDto;
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
}
